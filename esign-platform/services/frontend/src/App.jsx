import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import UploadPage from './pages/UploadPage';
import DocumentsPage from './pages/DocumentsPage';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header>
          <h1>E-Sign Platform</h1>
          <nav>
            <a href="/">Upload</a>
            <a href="/documents">Documents</a>
          </nav>
        </header>

        <main>
          <Routes>
            <Route path="/" element={<UploadPage />} />
            <Route path="/documents" element={<DocumentsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
