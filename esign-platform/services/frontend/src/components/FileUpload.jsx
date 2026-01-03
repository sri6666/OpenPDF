import React, { useRef } from 'react';

function FileUpload({ onUpload, disabled = false }) {
  const fileInputRef = useRef(null);

  const handleChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        alert('Please select a PDF file');
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        alert('File size must be less than 10MB');
        return;
      }
      onUpload(file);
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="file-upload">
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf"
        onChange={handleChange}
        disabled={disabled}
        style={{ display: 'none' }}
      />
      <button onClick={handleClick} disabled={disabled}>
        Choose PDF File
      </button>
    </div>
  );
}

export default FileUpload;
