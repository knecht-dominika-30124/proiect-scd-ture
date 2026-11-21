import axios from 'axios'

// toate cererile către server pleacă de aici
export const api = axios.create({
    baseURL: 'http://localhost:8083',
})