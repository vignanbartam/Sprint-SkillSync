import { Directive, TemplateRef, ViewContainerRef, inject, input } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Directive({
  selector: '[appRole]',
  standalone: true,
})
export class Role {
  readonly appRole = input.required<string>();

  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly authService = inject(AuthService);

  ngOnInit() {
    if (this.authService.hasRole(this.appRole())) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      return;
    }

    this.viewContainer.clear();
  }
}
