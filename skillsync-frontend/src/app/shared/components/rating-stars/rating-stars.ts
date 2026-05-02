import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-rating-stars',
  imports: [],
  templateUrl: './rating-stars.html',
  styleUrl: './rating-stars.scss',
})
export class RatingStars {
  readonly rating = input(0);
  protected readonly stars = computed(() =>
    Array.from({ length: 5 }, (_, index) => index < Math.round(this.rating())),
  );
}
