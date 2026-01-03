import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

class DocumentService {
  async upload(file) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await axios.post(`${API_BASE_URL}/documents/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });

    return response.data;
  }

  async list(page = 1, limit = 10) {
    const response = await axios.get(`${API_BASE_URL}/documents`, {
      params: { page, limit }
    });
    return response.data;
  }

  async get(id) {
    const response = await axios.get(`${API_BASE_URL}/documents/${id}`);
    return response.data;
  }

  async delete(id) {
    await axios.delete(`${API_BASE_URL}/documents/${id}`);
  }
}

export const documentService = new DocumentService();
