export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  age: number;
  email: string;
  password: string;
}

export interface JwtPayload {
  sub: string;
  role: string;
  userId: number;
  exp: number;
  iat: number;
}
