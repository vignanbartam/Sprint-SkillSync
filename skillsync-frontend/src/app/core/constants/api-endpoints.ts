export const API_ENDPOINTS = {
  auth: {
    login: '/authservice/auth/login',
    register: '/authservice/auth/register',
    verifyRegistration: '/authservice/auth/register/verify',
    userById: (id: number) => `/authservice/auth/user/${id}`,
    currentProfile: '/authservice/auth/profile',
    updateOwnRole: (role: string) => `/authservice/auth/profile/role/${role}`,
    uploadProfilePicture: '/authservice/auth/profile/picture',
    uploadBiodata: '/authservice/auth/profile/biodata',
    mentors: '/authservice/auth/mentors',
    profilePictureByUser: (id: number) => `/authservice/auth/profile-picture/${id}`,
    biodataByUser: (id: number) => `/authservice/auth/biodata/${id}`,
  },

  mentors: {
    apply: '/authservice/user/mentor-application',
    myApplications: '/authservice/user/mentor-application',
    revokeApplication: (id: number) => `/authservice/user/mentor-application/${id}`,
    updateSkills: '/authservice/user/mentor-skills',
  },

  sessions: {
    base: '/sessionservice/session',
    updateStatus: (id: number, status: string) =>
      `/sessionservice/session/${id}/${status}`,
    complete: (id: number) =>
      `/sessionservice/session/${id}/complete`,
    delete: (id: number) =>
      `/sessionservice/session/${id}`,
    completedCheck: '/sessionservice/session/check',
    mentorSessions: '/sessionservice/session/mentor',
    userSessions: '/sessionservice/session/user',
    adminSessions: '/sessionservice/session/admin',
  },

  groups: {
    base: '/groupservice/groups',
    mine: '/groupservice/groups/mine',
    messages: (id: number) => `/groupservice/groups/${id}/messages`,
    members: (id: number) => `/groupservice/groups/${id}/members`,
    join: (id: number) => `/groupservice/groups/${id}/join`,
    addMember: (id: number, userId: number) =>
      `/groupservice/groups/${id}/members/${userId}`,
    approve: (id: number, userId: number) =>
      `/groupservice/groups/${id}/approve/${userId}`,
    remove: (id: number, userId: number) =>
      `/groupservice/groups/${id}/members/${userId}`,
    delete: (id: number) => `/groupservice/groups/${id}`,
  },

  admin: {
    listMentorApplications: '/authservice/admin/mentor-applications',
    users: '/authservice/admin/users',
    deleteUser: (id: number) => `/authservice/admin/users/${id}`,
    updateUserRole: (id: number, role: string) =>
      `/authservice/admin/users/${id}/role/${role}`,
    approveMentor: (id: number) =>
      `/authservice/admin/mentors/${id}/approve`,
    rejectMentor: (id: number) =>
      `/authservice/admin/mentors/${id}/reject`,
    addSkill: '/skillservice/admin/skills',
    deleteSkill: (id: number) => `/skillservice/admin/skills/${id}`,
  },

  skills: {
    base: '/skillservice/skills',
  },

  reviews: {
    base: '/reviewservice/reviews',
    byMentor: (mentorId: number) =>
      `/reviewservice/reviews/mentor/${mentorId}`,
  },

  notifications: {
    byUser: (userId: number) =>
      `/notificationservice/notifications/${userId}`,
    delete: (id: number) =>
      `/notificationservice/notifications/${id}`,
  },
};
