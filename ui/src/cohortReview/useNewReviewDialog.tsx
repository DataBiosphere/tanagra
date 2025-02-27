import { useState } from "react";
import { UseNewReviewDialogProps, NewReviewDialog } from "./newReviewDialog";

export function useNewReviewDialog(
  props: UseNewReviewDialogProps
): [JSX.Element, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => setOpen(true);

  return [
    // eslint-disable-next-line react/jsx-key
    <NewReviewDialog open={open} setOpen={setOpen} {...props} />,
    show,
  ];
}
