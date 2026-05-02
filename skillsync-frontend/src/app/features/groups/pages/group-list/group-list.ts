import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/services/auth.service';
import { Group, GroupMember, GroupMessage, GroupService } from '../../services/group';

@Component({
  selector: 'app-group-list',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './group-list.html',
  styleUrl: './group-list.scss',
})
export class GroupList {
  private readonly groupService = inject(GroupService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly groups = signal<Group[]>([]);
  protected readonly myGroups = signal<Group[]>([]);
  protected readonly selectedGroup = signal<Group | null>(null);
  protected readonly managedGroup = signal<Group | null>(null);
  protected readonly managedMembers = signal<GroupMember[]>([]);
  protected readonly messages = signal<GroupMessage[]>([]);
  protected readonly chatStatus = signal('Select one of your groups to start chatting.');
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  private readonly userNameCache = new Map<number, string>();

  private stompClient?: Client;
  private groupSubscription?: StompSubscription;

  protected readonly createForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    description: ['', [Validators.required]],
  });

  protected readonly messageForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  constructor() {
    this.loadGroups();
    this.loadMyGroups();
  }

  ngOnDestroy() {
    this.disconnectChat();
  }

  loadGroups() {
    this.groupService.getAll().subscribe({
      next: (groups) => this.groups.set(groups),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  loadMyGroups() {
    this.groupService.getMine().subscribe({
      next: (groups) => {
        this.myGroups.set(groups);
        const selected = this.selectedGroup();
        if (selected && !groups.some((group) => group.id === selected.id)) {
          this.selectedGroup.set(null);
          this.messages.set([]);
          this.disconnectChat();
        }
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  createGroup() {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.groupService.create(this.createForm.getRawValue()).subscribe({
      next: () => {
        this.successMessage.set('Group created successfully.');
        this.loadGroups();
        this.loadMyGroups();
        this.createForm.reset({ name: '', description: '' });
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  joinGroup(groupId: number) {
    this.groupService.join(groupId).subscribe({
      next: () => {
        this.successMessage.set('Joined group.');
        this.loadGroups();
        this.loadMyGroups();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  manageGroup(group: Group) {
    if (!this.canManageGroup(group)) {
      return;
    }

    this.managedGroup.set(group);
    this.managedMembers.set([]);
    this.groupService.getMembers(group.id).subscribe({
      next: (members) => this.decorateMembers(members),
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  removeMember(groupId: number, userId: number) {
    this.groupService.remove(groupId, userId).subscribe({
      next: (message) => {
        this.successMessage.set(message);
        const group = this.managedGroup();
        if (group?.id === groupId) {
          this.manageGroup(group);
        }
        this.loadMyGroups();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  deleteGroup(group: Group) {
    if (!this.canManageGroup(group)) {
      return;
    }

    this.groupService.deleteGroup(group.id).subscribe({
      next: () => {
        this.successMessage.set('Group deleted.');
        if (this.selectedGroup()?.id === group.id) {
          this.selectedGroup.set(null);
          this.messages.set([]);
          this.disconnectChat();
        }
        if (this.managedGroup()?.id === group.id) {
          this.managedGroup.set(null);
          this.managedMembers.set([]);
        }
        this.loadGroups();
        this.loadMyGroups();
      },
      error: (error: Error) => this.errorMessage.set(error.message),
    });
  }

  selectGroup(group: Group) {
    this.selectedGroup.set(group);
    this.messages.set([]);
    this.chatStatus.set('Loading messages...');

    this.groupService.getMessages(group.id).subscribe({
      next: (messages) => {
        this.decorateMessages(messages);
        this.connectChat(group.id);
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.chatStatus.set('Unable to open this group chat.');
      },
    });
  }

  sendMessage() {
    const group = this.selectedGroup();
    if (!group || this.messageForm.invalid || !this.stompClient?.connected) {
      this.messageForm.markAllAsTouched();
      return;
    }

    const { content } = this.messageForm.getRawValue();
    this.stompClient.publish({
      destination: `/app/groups/${group.id}/send`,
      body: JSON.stringify({ content }),
    });
    this.messageForm.reset({ content: '' });
  }

  private connectChat(groupId: number) {
    this.disconnectChat();

    const token = this.authService.token();
    if (!token) {
      this.chatStatus.set('Sign in again to connect to chat.');
      return;
    }

    this.stompClient = new Client({
      brokerURL: this.buildWebSocketUrl(token),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.chatStatus.set('Connected');
        this.groupSubscription = this.stompClient?.subscribe(
          `/topic/groups/${groupId}`,
          (message) => this.appendMessage(message),
        );
      },
      onStompError: (frame) => {
        this.chatStatus.set(frame.headers['message'] || 'Chat connection error.');
      },
      onWebSocketClose: () => {
        if (this.selectedGroup()?.id === groupId) {
          this.chatStatus.set('Chat disconnected. Reconnecting...');
        }
      },
    });

    this.chatStatus.set('Connecting...');
    this.stompClient.activate();
  }

  private appendMessage(message: IMessage) {
    const nextMessage = JSON.parse(message.body) as GroupMessage;
    this.decorateMessage(nextMessage, (decorated) => {
      this.messages.update((messages) => [...messages, decorated]);
    });
  }

  private decorateMessages(messages: GroupMessage[]) {
    if (!messages.length) {
      this.messages.set([]);
      return;
    }

    forkJoin(
      messages.map((message) =>
        this.resolveUserName(message.senderId).pipe(
          map((name) => ({ ...message, senderName: name })),
          catchError(() => of(message)),
        ),
      ),
    ).subscribe((decorated) => this.messages.set(decorated));
  }

  private decorateMessage(message: GroupMessage, callback: (message: GroupMessage) => void) {
    this.resolveUserName(message.senderId).pipe(
      map((name) => ({ ...message, senderName: name })),
      catchError(() => of(message)),
    ).subscribe(callback);
  }

  private resolveUserName(userId: number) {
    const cached = this.userNameCache.get(userId);
    if (cached) {
      return of(cached);
    }

    return this.authService.getUserById(userId).pipe(
      map((user) => {
        const name = user.name || user.email || `User ${this.displayId(userId)}`;
        this.userNameCache.set(userId, name);
        return name;
      }),
    );
  }

  private disconnectChat() {
    this.groupSubscription?.unsubscribe();
    this.groupSubscription = undefined;
    void this.stompClient?.deactivate();
    this.stompClient = undefined;
  }

  private buildWebSocketUrl(token: string) {
    const wsBaseUrl = environment.apiBaseUrl.replace(/^http/, 'ws');
    return `${wsBaseUrl}/groupservice/ws?access_token=${encodeURIComponent(token)}`;
  }

  protected readonly currentUser = this.authService.profile;
  protected readonly currentRole = this.authService.role;

  protected canManageGroup(group: Group) {
    const user = this.currentUser();
    return this.currentRole() === 'ROLE_ADMIN' || (!!user && group.createdBy === user.id);
  }

  protected alreadyJoined(group: Group) {
    return this.myGroups().some((mine) => mine.id === group.id);
  }

  protected displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }

  private decorateMembers(members: GroupMember[]) {
    if (!members.length) {
      this.managedMembers.set([]);
      return;
    }

    forkJoin(
      members.map((member) =>
        this.authService.getUserById(member.userId).pipe(
          map((user) => ({
            ...member,
            displayName: user.name || user.email || member.displayName,
          })),
          catchError(() => of(member)),
        ),
      ),
    ).subscribe((decorated) => this.managedMembers.set(decorated));
  }
}
