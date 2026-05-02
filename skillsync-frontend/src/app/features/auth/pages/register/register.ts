import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { Loader } from '../../../../shared/components/loader/loader';

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, Loader],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');
  protected readonly submitted = signal(false);
  protected readonly otpSent = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    age: [18, [Validators.required, Validators.min(1)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    otp: [''],
  });

  submit() {
    this.submitted.set(true);

    if (this.form.invalid) {
      this.errorMessage.set('Fix the highlighted fields before creating the account.');
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.authService.register(this.form.getRawValue()).subscribe({
      next: (message) => {
        this.loading.set(false);
        this.successMessage.set(message);
        this.otpSent.set(true);
      },
      error: (error: Error) => {
        this.loading.set(false);
        this.errorMessage.set(error.message);
      },
    });
  }

  verifyOtp() {
    const { email, otp } = this.form.getRawValue();
    if (!email || !otp.trim()) {
      this.errorMessage.set('Enter the OTP sent to your email.');
      return;
    }

    this.loading.set(true);
    this.authService.verifyRegistration({ email, otp }).subscribe({
      next: (message) => {
        this.loading.set(false);
        this.successMessage.set(message);
        setTimeout(() => void this.router.navigate(['/login']), 900);
      },
      error: (error: Error) => {
        this.loading.set(false);
        this.errorMessage.set(error.message);
      },
    });
  }

  protected hasError(controlName: 'name' | 'age' | 'email' | 'password') {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || this.submitted());
  }
}
