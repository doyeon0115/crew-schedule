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

export type UserStatus = "ACTIVE" | "SUSPENDED";
export type ReportTargetType = "POST" | "COMMENT" | "CHAT_MESSAGE" | "USER";
export type ReportStatus = "PENDING" | "RESOLVED" | "DISMISSED";

export type DashboardStats = {
  totalUsers: number;
  activeUsers: number;
  suspendedUsers: number;
  adminUsers: number;
  totalCrews: number;
  totalMeetups: number;
  pendingReports: number;
};

export type AdminUser = {
  id: number;
  email: string;
  nickname: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
};

export type AdminUserList = {
  users: AdminUser[];
};

export type AdminReport = {
  id: number;
  reporterId: number;
  targetType: ReportTargetType;
  targetId: number;
  reason: string;
  status: ReportStatus;
  handledBy: number | null;
  handledAt: string | null;
  adminMemo: string | null;
  createdAt: string;
};

export type AdminReportList = {
  reports: AdminReport[];
  pendingCount: number;
};

export type PollStatus = "OPEN" | "CLOSED";

export type PollCandidate = {
  id: number;
  date: string; // "YYYY-MM-DD"
  startTime: string | null;
  voteCount: number;
  voterIds: number[];
  votedByMe: boolean;
};

export type Poll = {
  id: number;
  crewId: number;
  creatorId: number;
  title: string;
  status: PollStatus;
  winnerCandidateId: number | null;
  closedAt: string | null;
  createdAt: string;
  candidates: PollCandidate[];
};

export type CreatePollInput = {
  title: string;
  candidates: { date: string; startTime: string | null }[];
};

export type PostSummary = {
  id: number;
  crewId: number;
  authorId: number;
  authorNickname: string;
  title: string;
  excerpt: string;
  commentCount: number;
  createdAt: string;
};

export type PostListResponse = {
  posts: PostSummary[];
  nextBeforeId: number | null;
};

export type ReactionSummary = {
  emoji: string;
  count: number;
  userIds: number[];
  myReaction: boolean;
};

export type CommentNode = {
  id: number;
  postId: number;
  parentCommentId: number | null;
  authorId: number | null;
  authorNickname: string;
  content: string;
  reactions: ReactionSummary[];
  replies: CommentNode[];
  createdAt: string;
};

export type PostDetail = {
  id: number;
  crewId: number;
  authorId: number;
  authorNickname: string;
  title: string;
  content: string;
  reactions: ReactionSummary[];
  comments: CommentNode[];
  createdAt: string;
  updatedAt: string;
};

export type CreatePostInput = { title: string; content: string };
export type CreateCommentInput = { content: string; parentCommentId?: number | null };
