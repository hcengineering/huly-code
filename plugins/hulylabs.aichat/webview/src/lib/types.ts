export interface Message {
  id: string;
  content: string;
  role: string;
  isError: boolean;
}