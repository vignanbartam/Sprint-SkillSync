import { Component, input } from '@angular/core';
import { RatingStars } from '../rating-stars/rating-stars';

@Component({
  selector: 'app-mentor-card',
  imports: [RatingStars],
  templateUrl: './mentor-card.html',
  styleUrl: './mentor-card.scss',
})
export class MentorCard {
  readonly mentorId = input.required<number>();
  readonly reviewCount = input(0);
  readonly averageRating = input(0);
  readonly skills = input<string[]>([]);

  displayId(id: number) {
    return String(((id * 7919) % 9000) + 1000);
  }
}
