// 백엔드 DTO(record)에 대응하는 TypeScript 타입.
// 백엔드에서 변경되면 여기도 같이 손봐야 함.

export type ApiResponse<T> = {
  code: string;
  message: string;
  data: T;
};

export type UserRole = "USER" | "ADMIN";
export type AuthProvider = "LOCAL" | "KAKAO" | "GOOGLE";

export type UserSummary = {
  id: number;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  role: UserRole;
  provider: AuthProvider;
};

export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  accessExpiresInSeconds: number;
  user: UserSummary;
};

export type CrewRole = "OWNER" | "MEMBER";

export type Crew = {
  id: number;
  name: string;
  inviteCode: string;
  ownerId: number;
  memberCount: number;
};

export type CrewMember = {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  role: CrewRole;
};

export type CrewDetail = {
  id: number;
  name: string;
  inviteCode: string;
  ownerId: number;
  members: CrewMember[];
};

// 백엔드는 java.time.DayOfWeek를 대문자 이름으로 직렬화. "MONDAY" 등.
export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

/** startTime/endTime은 "HH:mm:ss" 문자열, off=true면 null. */
export type SlotResponse = {
  dayOfWeek: DayOfWeek;
  off: boolean;
  startTime: string | null;
  endTime: string | null;
};

export type WeeklySchedule = {
  userId: number;
  slots: SlotResponse[];
};

export type MemberSchedule = {
  userId: number;
  nickname: string;
  profileImageUrl: string | null;
  slots: SlotResponse[];
};

export type CrewScheduleBoard = {
  crewId: number;
  members: MemberSchedule[];
};

/** availableFrom은 allDayFree=true면 null, 아니면 "HH:mm:ss". */
export type DayRecommendation = {
  date: string; // "YYYY-MM-DD"
  dayOfWeek: DayOfWeek;
  allDayFree: boolean;
  availableFrom: string | null;
  offCount: number;
  rank: number;
};

export type SlotUpdate = {
  dayOfWeek: DayOfWeek;
  off: boolean;
  startTime: string | null;
  endTime: string | null;
};

export type Rsvp = "PENDING" | "ATTEND" | "MAYBE" | "ABSENT";
export type MeetupStatus = "PROPOSED" | "CONFIRMED" | "CANCELED";

export type MeetupParticipant = {
  userId: number;
  nickname: string;
  rsvp: Rsvp;
};

export type Meetup = {
  id: number;
  crewId: number;
  creatorId: number;
  title: string;
  meetDate: string; // "YYYY-MM-DD"
  startTime: string; // "HH:mm:ss"
  location: string | null;
  memo: string | null;
  status: MeetupStatus;
  capacity: number | null;
  currentParticipants: number;
  participants: MeetupParticipant[];
};

export type CreateMeetupInput = {
  title: string;
  meetDate: string;
  startTime: string;
  location?: string | null;
  memo?: string | null;
  participantUserIds?: number[];
  capacity?: number | null;
};

export type ChatMessage = {
  id: number;
  crewId: number;
  senderId: number;
  senderNickname: string;
  content: string;
  sentAt: string; // ISO
};

export type ChatHistory = {
  messages: ChatMessage[];
  nextBeforeId: number | null;
};

export type NotificationType =
  | "MEETUP_PROPOSED"
  | "MEETUP_JOINED"
  | "MEETUP_CONFIRMED"
  | "POLL_CREATED"
  | "POLL_CLOSED";

export type NotificationItem = {
  id: number;
  type: NotificationType;
  crewId: number | null;
  actorId: number | null;
  payload: string; // JSON string
  read: boolean;
  createdAt: string;
};

export type NotificationPage = {
  notifications: NotificationItem[];
  nextBeforeId: number | null;
  unreadCount: number;
};
