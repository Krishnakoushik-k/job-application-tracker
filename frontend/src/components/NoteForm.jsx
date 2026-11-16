import { useState } from 'react';
import { SelectField, TextAreaField } from './FormFields';
import { NOTE_CATEGORY_OPTIONS } from '../utils/constants';
import { hasErrors, validateNote } from '../utils/validators';

export default function NoteForm({ initialValues, onSubmit, onCancel, submitting, serverErrors = {} }) {
  const [values, setValues] = useState({
    category: initialValues?.category ?? 'GENERAL',
    content: initialValues?.content ?? '',
  });
  const [clientErrors, setClientErrors] = useState({});
  const errors = { ...serverErrors, ...clientErrors };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
    setClientErrors((current) => ({ ...current, [name]: undefined }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const validation = validateNote(values);
    setClientErrors(validation);
    if (hasErrors(validation)) return;
    onSubmit({ category: values.category, content: values.content.trim() });
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="note-form">
      <SelectField id="noteCategory" name="category" label="Category" options={NOTE_CATEGORY_OPTIONS}
        value={values.category} onChange={handleChange} error={errors.category} />
      <TextAreaField id="noteContent" name="content" label="Note" rows={3} required value={values.content}
        onChange={handleChange} error={errors.content} maxLength={5000} />
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        )}
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving…' : initialValues?.id ? 'Save note' : 'Add note'}
        </button>
      </div>
    </form>
  );
}
