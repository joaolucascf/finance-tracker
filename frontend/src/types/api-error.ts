export type ApiError = {
  message: string;
  fields?: FieldError[];
};

export type FieldError = {
  field: string;
  message: string;
};
