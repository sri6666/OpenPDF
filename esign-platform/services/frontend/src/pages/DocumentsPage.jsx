import React, { useState, useEffect } from 'react';
import { documentService } from '../services/documentService';

function DocumentsPage() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      const data = await documentService.list();
      setDocuments(data.documents);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (confirm('Are you sure you want to delete this document?')) {
      try {
        await documentService.delete(id);
        setDocuments(documents.filter(doc => doc._id !== id));
      } catch (err) {
        alert('Failed to delete document');
      }
    }
  };

  if (loading) return <p>Loading...</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <div className="documents-page">
      <h2>My Documents</h2>

      {documents.length === 0 ? (
        <p>No documents yet. Upload your first PDF!</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Filename</th>
              <th>Size</th>
              <th>Status</th>
              <th>Uploaded</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {documents.map(doc => (
              <tr key={doc._id}>
                <td>{doc.fileName}</td>
                <td>{(doc.fileSize / 1024).toFixed(1)} KB</td>
                <td>{doc.status}</td>
                <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                <td>
                  <button onClick={() => handleDelete(doc._id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default DocumentsPage;
