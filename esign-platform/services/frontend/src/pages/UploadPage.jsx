import React, { useState } from 'react';
import FileUpload from '../components/FileUpload';
import { documentService } from '../services/documentService';

function UploadPage() {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleUpload = async (file) => {
    setUploading(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await documentService.upload(file);
      setSuccess(`File uploaded successfully! Document ID: ${result.documentId}`);
    } catch (err) {
      setError(err.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="upload-page">
      <h2>Upload PDF Document</h2>

      <FileUpload onUpload={handleUpload} disabled={uploading} />

      {uploading && <p className="loading">Uploading...</p>}
      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}
    </div>
  );
}

export default UploadPage;
