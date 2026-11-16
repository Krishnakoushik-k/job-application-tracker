import Modal from './Modal';

/** Confirmation required before any destructive action. */
export default function ConfirmDialog({ title, message, confirmLabel = 'Delete', onConfirm, onCancel, busy = false }) {
  return (
    <Modal title={title} onClose={busy ? () => {} : onCancel}>
      <p className="confirm-message">{message}</p>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={busy}>
          Cancel
        </button>
        <button type="button" className="btn btn-danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Working…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
