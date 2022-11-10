import CircularProgress from "@mui/material/CircularProgress";
import Modal from "@mui/material/Modal";
import Stack from "@mui/material/Stack";

export default function LoadingOverlay() {
  return (
    <Modal open sx={{ zIndex: (theme) => theme.zIndex.appBar - 1 }}>
      <Stack
        alignItems="center"
        justifyContent="center"
        sx={{
          width: "100%",
          height: "100%",
        }}
      >
        <CircularProgress />
      </Stack>
    </Modal>
  );
}
