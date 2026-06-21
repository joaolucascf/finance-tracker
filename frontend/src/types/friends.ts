export type Friend = {
  id: number;
  name: string;
  email: string;
  photoBase64: string | null;
  photoType: string | null;
};

export type FriendRequestItem = {
  id: number;
  name: string;
  email: string;
  photoBase64: string | null;
  photoType: string | null;
};

export type FriendRequestResponse = {
  id: number;
  name: string;
  email: string;
  status: string;
};
