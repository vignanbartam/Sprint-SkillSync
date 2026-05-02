export interface User {
  id: number;
  email: string;
  role?: string;
  name?: string;
  age?: number;
  dob?: string | null;
  address?: string | null;
  phoneNumber?: string | null;
  about?: string | null;
  linkedinUrl?: string | null;
  xUrl?: string | null;
  instagramUrl?: string | null;
  mentorExperience?: number | null;
  skillIds?: number[] | null;
  quickSessionPrice?: number | null;
  focusedSessionPrice?: number | null;
  deepSessionPrice?: number | null;
  mentorApproved?: boolean;
  hasProfilePicture?: boolean;
  hasBiodata?: boolean;
  biodataFileName?: string | null;
  profilePictureUrl?: string | null;
  biodataUrl?: string | null;
}

export interface AuthUser extends User {
  token: string;
}
