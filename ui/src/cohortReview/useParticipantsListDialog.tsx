import React, { ReactNode, useState } from "react";
import {
  ParticipantsListDialogProps,
  ParticipantsListDialog,
} from "cohortReview/participantsListDialog";

export function useParticipantsListDialog(
  props: ParticipantsListDialogProps
): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => setOpen(true);

  return [
    // eslint-disable-next-line react/jsx-key
    <ParticipantsListDialog
      {...props}
      open={open}
      hide={() => setOpen(false)}
    />,
    show,
  ];
}
