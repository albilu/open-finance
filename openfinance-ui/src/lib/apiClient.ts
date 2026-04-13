import axios from 'axios';

const apiClient = axios.create({
    baseURL: '', // The proxy in vite.config.ts handles the /api requests
    headers: {
        'Content-Type': 'application/json',
    },
});

export default apiClient;
