// API Configuration with caching
// Dynamically set API URL based on environment
const API_BASE_URL = window.location.hostname === 'localhost' 
    ? 'http://localhost:8080/api' 
    : `${window.location.protocol}//${window.location.hostname}${window.location.port ? ':' + window.location.port : ''}/api`;

console.log('API Base URL:', API_BASE_URL);

const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
const QUIZ_CACHE_DURATION = 10 * 60 * 1000; // 10 minutes for quiz data
const cache = new Map();

// Cache helper
const getCached = (key) => {
    const cached = cache.get(key);
    if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
        return cached.data;
    }
    return null;
};

const setCache = (key, data) => {
    cache.set(key, { data, timestamp: Date.now() });
};

// Token Management
const TokenManager = {
    getToken: () => localStorage.getItem('token'),
    setToken: (token) => localStorage.setItem('token', token),
    removeToken: () => localStorage.removeItem('token'),
    getUsername: () => localStorage.getItem('username'),
    setUsername: (username) => localStorage.setItem('username', username),
    removeUsername: () => localStorage.removeItem('username'),
    getUserId: () => localStorage.getItem('userId'),
    setUserId: (userId) => localStorage.setItem('userId', userId),
    removeUserId: () => localStorage.removeItem('userId')
};

// Auth Check
function checkAuth() {
    const token = TokenManager.getToken();
    return !!token;
}

// API Client with better error handling and retry logic
const apiClient = {
    handleError: async (response) => {
        if (!response.ok) {
            let errorMessage = 'An error occurred';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorData.error || errorMessage;
            } catch (e) {
                errorMessage = `HTTP ${response.status}: ${response.statusText}`;
            }
            
            if (response.status === 401) {
                console.error('Authentication failed');
                TokenManager.removeToken();
                window.location.href = '/';
                throw new Error('Authentication failed');
            } else if (response.status === 400) {
                throw new Error(`Bad Request: ${errorMessage}`);
            } else if (response.status === 404) {
                throw new Error(`Not Found: ${errorMessage}`);
            } else if (response.status === 500) {
                throw new Error(`Server Error: ${errorMessage}`);
            } else {
                throw new Error(errorMessage);
            }
        }
        return response;
    },
    
    get: async (url, options = {}) => {
        try {
            const response = await fetch(`${API_BASE_URL}${url}`, {
                ...options,
                headers: {
                    'Authorization': `Bearer ${TokenManager.getToken()}`,
                    ...options.headers
                }
            });
            
            return apiClient.handleError(response);
        } catch (error) {
            console.error('API GET Error:', error);
            throw error;
        }
    },
    
    post: async (url, data, options = {}) => {
        try {
            const response = await fetch(`${API_BASE_URL}${url}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${TokenManager.getToken()}`,
                    ...options.headers
                },
                body: JSON.stringify(data),
                ...options
            });
            
            return apiClient.handleError(response);
        } catch (error) {
            console.error('API POST Error:', error);
            throw error;
        }
    },
    
    put: async (url, data, options = {}) => {
        try {
            const response = await fetch(`${API_BASE_URL}${url}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${TokenManager.getToken()}`,
                    ...options.headers
                },
                body: JSON.stringify(data),
                ...options
            });
            
            return apiClient.handleError(response);
        } catch (error) {
            console.error('API PUT Error:', error);
            throw error;
        }
    },
    
    delete: async (url, options = {}) => {
        try {
            const response = await fetch(`${API_BASE_URL}${url}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getToken()}`,
                    ...options.headers
                },
                ...options
            });
            
            return apiClient.handleError(response);
        } catch (error) {
            console.error('API DELETE Error:', error);
            throw error;
        }
    },
    
    ai: {
        generateStudyPlan: async (data) => {
            const response = await apiClient.post('/ai/study-plan', data);
            if (!response.ok) throw new Error('Failed to generate study plan');
            return await response.text();
        }
    },
    
    studyPlans: {
        create: async (planData) => {
            const response = await apiClient.post('/study-plans', planData);
            if (!response.ok) throw new Error('Failed to create study plan');
            
            // Check if response has content
            const text = await response.text();
            if (!text) {
                console.warn('Empty response from server, returning default response');
                // Return a default response with the sent data
                return {
                    id: Date.now(),
                    ...planData,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString()
                };
            }
            
            try {
                return JSON.parse(text);
            } catch (e) {
                console.error('Failed to parse response:', text);
                throw new Error('Invalid response from server');
            }
        },
        
        getAll: async () => {
            const response = await apiClient.get('/study-plans');
            if (!response.ok) throw new Error('Failed to fetch study plans');
            return await response.json();
        },
        
        getByDateRange: async (startDate, endDate) => {
            const response = await apiClient.get(`/study-plans/range?startDate=${startDate}&endDate=${endDate}`);
            if (!response.ok) throw new Error('Failed to fetch study plans');
            return await response.json();
        },
        
        update: async (id, planData) => {
            const response = await apiClient.put(`/study-plans/${id}`, planData);
            if (!response.ok) throw new Error('Failed to update study plan');
            
            // Check if response has content
            const text = await response.text();
            if (!text) {
                console.warn('Empty response from server, returning default response');
                // Return a default response with the sent data
                return {
                    id: id,
                    ...planData,
                    updatedAt: new Date().toISOString()
                };
            }
            
            try {
                return JSON.parse(text);
            } catch (e) {
                console.error('Failed to parse response:', text);
                throw new Error('Invalid response from server');
            }
        },
        
        delete: async (id) => {
            const response = await apiClient.delete(`/study-plans/${id}`);
            if (!response.ok) throw new Error('Failed to delete study plan');
            return response;
        }
    },
    
    friends: {
        searchUsers: async (query) => {
            const response = await apiClient.get(`/friends/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Search failed');
            }
            return await response.json();
        },
        
        getFriends: async () => {
            const response = await apiClient.get('/friends');
            if (!response.ok) throw new Error('Failed to fetch friends');
            return await response.json();
        },
        
        getRequests: async () => {
            const response = await apiClient.get('/friends/requests');
            if (!response.ok) throw new Error('Failed to fetch friend requests');
            return await response.json();
        },
        
        sendRequest: async (targetUserId) => {
            const response = await apiClient.post(`/friends/request/${targetUserId}`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to send friend request');
            }
            return await response.json();
        },
        
        acceptRequest: async (requestId) => {
            const response = await apiClient.post(`/friends/accept/${requestId}`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to accept friend request');
            }
            return await response.json();
        },
        
        declineRequest: async (requestId) => {
            const response = await apiClient.post(`/friends/decline/${requestId}`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to decline friend request');
            }
            return await response.json();
        },
        
        removeFriend: async (friendId) => {
            const response = await apiClient.delete(`/friends/${friendId}`);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to remove friend');
            }
            return await response.json();
        },
        
        getRecommendations: async (limit = 10) => {
            const response = await apiClient.get(`/friends/recommendations?limit=${limit}`);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to get recommendations');
            }
            return await response.json();
        }
    },
    
    groups: {
        createGroup: async (groupData) => {
            const response = await apiClient.post('/groups', groupData);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to create group');
            }
            return await response.json();
        },
        
        getMyGroups: async () => {
            const response = await apiClient.get('/groups/my');
            if (!response.ok) throw new Error('Failed to fetch my groups');
            return await response.json();
        },
        
        getPublicGroups: async () => {
            const response = await apiClient.get('/groups/public');
            if (!response.ok) throw new Error('Failed to fetch public groups');
            return await response.json();
        },
        
        searchGroups: async (query) => {
            const response = await apiClient.get(`/groups/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('Failed to search groups');
            return await response.json();
        },
        
        joinGroup: async (groupId) => {
            const response = await apiClient.post(`/groups/${groupId}/join`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to join group');
            }
            return await response.json();
        },
        
        leaveGroup: async (groupId) => {
            const response = await apiClient.post(`/groups/${groupId}/leave`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to leave group');
            }
            return await response.json();
        },
        
        sendMessage: async (groupId, content) => {
            const response = await apiClient.post(`/groups/${groupId}/messages`, { content });
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to send message');
            }
            return await response.json();
        },
        
        getMessages: async (groupId, page = 0, size = 50) => {
            const response = await apiClient.get(`/groups/${groupId}/messages?page=${page}&size=${size}`);
            if (!response.ok) throw new Error('Failed to fetch messages');
            return await response.json();
        },
        
        getGroupDetails: async (groupId) => {
            const response = await apiClient.get(`/groups/${groupId}`);
            if (!response.ok) throw new Error('Failed to fetch group details');
            return await response.json();
        },
        
        getMembers: async (groupId) => {
            const response = await apiClient.get(`/groups/${groupId}/members`);
            if (!response.ok) throw new Error('Failed to fetch group members');
            return await response.json();
        }
    }
};

// API Methods with improved error handling
const API = {
    auth: {
        login: async (credentials) => {
            try {
                console.log('Attempting login with:', credentials.email);
                const response = await fetch(`${API_BASE_URL}/auth/login`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(credentials)
                });
                
                console.log('Login response status:', response.status);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('Login failed:', errorText);
                    throw new Error(errorText || 'Login failed');
                }
                
                const data = await response.json();
                console.log('Login successful, received data:', data);
                
                // Handle different token field names
                const token = data.accessToken || data.access_token || data.token;
                if (token) {
                    TokenManager.setToken(token);
                } else {
                    console.error('No token in response:', data);
                    throw new Error('No authentication token received');
                }
                
                TokenManager.setUsername(credentials.email.split('@')[0]);
                if (data.userId || data.user_id || data.id) {
                    TokenManager.setUserId(data.userId || data.user_id || data.id);
                }
                return data;
            } catch (error) {
                console.error('Login error:', error);
                throw error;
            }
        },
        
        signup: async (userData) => {
            try {
                console.log('Attempting signup with:', userData.email);
                const response = await fetch(`${API_BASE_URL}/auth/signup`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(userData)
                });
                
                console.log('Signup response status:', response.status);
                
                if (!response.ok) {
                    const error = await response.json();
                    console.error('Signup failed:', error);
                    throw new Error(error.message || 'Signup failed');
                }
                
                return await response.json();
            } catch (error) {
                console.error('Signup error:', error);
                throw error;
            }
        },
        
        logout: () => {
            TokenManager.removeToken();
            TokenManager.removeUsername();
            TokenManager.removeUserId();
            window.location.href = '/';
        }
    },
    
    studyMaterials: {
        upload: async (file, title, courseId = null, progressCallback) => {
            const formData = new FormData();
            formData.append("file", file);
            formData.append("title", title);
            if (courseId) formData.append("courseId", courseId);

            const response = await fetch(`${API_BASE_URL}/materials/upload`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getToken()}`
                },
                body: formData
            });

            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || 'Upload failed');
            }

            return await response.json();
        },
        
        getAll: async (forceRefresh = false) => {
            const cacheKey = 'materials_list';
            if (!forceRefresh) {
                const cached = getCached(cacheKey);
                if (cached) return cached;
            }
            
            const response = await apiClient.get('/materials');
            if (!response.ok) throw new Error('Failed to fetch materials');
            const data = await response.json();
            setCache(cacheKey, data);
            return data;
        },
        
        getById: async (id) => {
            const response = await apiClient.get(`/materials/${id}`);
            if (!response.ok) throw new Error('Failed to fetch material');
            return await response.json();
        },
        
        generateQuizzes: async (id, count = 5, difficulty = 'MEDIUM') => {
            const response = await fetch(`${API_BASE_URL}/materials/${id}/quizzes?count=${count}&difficulty=${difficulty}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getToken()}`
                }
            });
            if (!response.ok) throw new Error('Failed to generate quiz');
            return await response.json();
        },
        
        delete: async (id) => {
            const response = await apiClient.delete(`/materials/${id}`);
            if (!response.ok) throw new Error('Failed to delete material');
            return response;
        }
    },
    
    materials: {
        upload: async (file, title, className = null, courseId = null, progressCallback) => {
            const formData = new FormData();
            formData.append("file", file);
            formData.append("title", title);
            if (className) formData.append("className", className);
            if (courseId) formData.append("courseId", courseId);

            const response = await fetch(`${API_BASE_URL}/materials/upload`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getToken()}`
                },
                body: formData
            });

            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || 'Upload failed');
            }

            return await response.json();
        },
        list: async () => {
            return API.studyMaterials.getAll();
        },
        getById: async (id) => {
            return API.studyMaterials.getById(id);
        },
        generateQuizzes: async (id, count, difficulty) => {
            return API.studyMaterials.generateQuizzes(id, count, difficulty);
        },
        delete: async (id) => {
            return API.studyMaterials.delete(id);
        }
    },
    
    quiz: {
        attempt: async (quizId, answers) => {
            const response = await apiClient.post(`/quizzes/${quizId}/attempts`, { answers });
            if (!response.ok) throw new Error('Failed to submit quiz');
            return await response.json();
        },
        
        getHistory: async () => {
            try {
                // 모든 퀴즈 가져오기
                const quizzes = await API.quizzes.list();
                
                // 각 퀴즈의 시도 기록을 수집
                const history = [];
                for (const quiz of quizzes) {
                    if (quiz.attempts && quiz.attempts > 0) {
                        history.push({
                            quizId: quiz.id,
                            materialTitle: quiz.material?.title || quiz.title,
                            attempts: quiz.attempts,
                            // 추가 정보가 필요하면 여기에 추가
                        });
                    }
                }
                
                return history;
            } catch (error) {
                console.error('Failed to fetch quiz history:', error);
                return [];
            }
        },
        
        getHistory: async (materialId) => {
            const response = await apiClient.get(`/quizzes/materials/${materialId}/history`);
            if (!response.ok) throw new Error('Failed to fetch quiz history');
            return await response.json();
        },
        
        getAttemptDetail: async (attemptId) => {
            const response = await apiClient.get(`/quizzes/attempts/${attemptId}`);
            if (!response.ok) throw new Error('Failed to fetch attempt detail');
            return await response.json();
        },
        
        getLastAttempt: async (materialId) => {
            const response = await apiClient.get(`/quizzes/materials/${materialId}/last-attempt`);
            if (!response.ok) throw new Error('Failed to fetch last attempt');
            return await response.json();
        }
    },
    
    quizzes: {
        list: async (forceRefresh = false) => {
            const cacheKey = 'quizzes_list';
            if (!forceRefresh) {
                const cached = getCached(cacheKey);
                if (cached) return cached;
            }
            
            try {
                const response = await apiClient.get('/quizzes');
                if (!response.ok) throw new Error('Failed to fetch quizzes');
                const data = await response.json();
                setCache(cacheKey, data);
                return data;
            } catch (error) {
                // If error occurs, try to return cached data if available
                const cached = cache.get(cacheKey);
                if (cached && cached.data) {
                    console.log('Returning cached data due to error:', error);
                    return cached.data;
                }
                throw error;
            }
        },
        
        get: async (id) => {
            const cacheKey = `quiz_${id}`;
            const cached = getCached(cacheKey);
            if (cached) return cached;
            
            const response = await apiClient.get(`/quizzes/${id}`);
            if (!response.ok) throw new Error('Failed to fetch quiz');
            const data = await response.json();
            // Cache for longer duration for quiz data
            cache.set(cacheKey, { data, timestamp: Date.now() });
            return data;
        },
        
        submitAttempt: async (quizId, attemptData) => {
            const response = await apiClient.post(`/quizzes/${quizId}/attempts`, attemptData);
            if (!response.ok) throw new Error('Failed to submit quiz');
            // Clear quiz list cache after submission
            cache.delete('quizzes_list');
            return await response.json();
        },
        
        delete: async (quizId) => {
            const response = await apiClient.delete(`/quizzes/${quizId}`);
            if (!response.ok) throw new Error('Failed to delete quiz');
            // Clear quiz list cache after deletion
            cache.delete('quizzes_list');
            return await response.json();
        }
    },
    
    courses: {
        create: async (courseData) => {
            const response = await apiClient.post('/courses', courseData);
            if (!response.ok) throw new Error('Failed to create course');
            return await response.json();
        },
        
        getAll: async () => {
            const response = await apiClient.get('/courses');
            if (!response.ok) throw new Error('Failed to fetch courses');
            return await response.json();
        }
    },
    
    dashboard: {
        getStatistics: async () => {
            const response = await apiClient.get('/statistics');
            if (!response.ok) throw new Error('Failed to fetch statistics');
            return await response.json();
        }
    },
    
    user: {
        deleteAccount: async () => {
            const response = await apiClient.delete('/auth/me');
            if (!response.ok) throw new Error('Failed to delete account');
            return await response.json();
        }
    },
    
    ai: {
        generateStudyPlan: async (data) => {
            const response = await apiClient.post('/ai/study-plan', data);
            if (!response.ok) throw new Error('Failed to generate study plan');
            return await response.text();
        }
    },
    
    studyPlans: {
        create: async (planData) => {
            const response = await apiClient.post('/study-plans', planData);
            if (!response.ok) throw new Error('Failed to create study plan');
            
            // Check if response has content
            const text = await response.text();
            if (!text) {
                console.warn('Empty response from server, returning default response');
                // Return a default response with the sent data
                return {
                    id: Date.now(),
                    ...planData,
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString()
                };
            }
            
            try {
                return JSON.parse(text);
            } catch (e) {
                console.error('Failed to parse response:', text);
                throw new Error('Invalid response from server');
            }
        },
        
        getAll: async () => {
            const response = await apiClient.get('/study-plans');
            if (!response.ok) throw new Error('Failed to fetch study plans');
            return await response.json();
        },
        
        getByDateRange: async (startDate, endDate) => {
            const response = await apiClient.get(`/study-plans/range?startDate=${startDate}&endDate=${endDate}`);
            if (!response.ok) throw new Error('Failed to fetch study plans');
            return await response.json();
        },
        
        update: async (id, planData) => {
            const response = await apiClient.put(`/study-plans/${id}`, planData);
            if (!response.ok) throw new Error('Failed to update study plan');
            
            // Check if response has content
            const text = await response.text();
            if (!text) {
                console.warn('Empty response from server, returning default response');
                // Return a default response with the sent data
                return {
                    id: id,
                    ...planData,
                    updatedAt: new Date().toISOString()
                };
            }
            
            try {
                return JSON.parse(text);
            } catch (e) {
                console.error('Failed to parse response:', text);
                throw new Error('Invalid response from server');
            }
        },
        
        delete: async (id) => {
            const response = await apiClient.delete(`/study-plans/${id}`);
            if (!response.ok) throw new Error('Failed to delete study plan');
            return response;
        }
    },
    
    friends: {
        searchUsers: async (query) => {
            const response = await apiClient.get(`/friends/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Search failed');
            }
            return await response.json();
        },
        
        getFriends: async () => {
            const response = await apiClient.get('/friends');
            if (!response.ok) throw new Error('Failed to fetch friends');
            return await response.json();
        },
        
        getRequests: async () => {
            const response = await apiClient.get('/friends/requests');
            if (!response.ok) throw new Error('Failed to fetch friend requests');
            return await response.json();
        },
        
        sendRequest: async (targetUserId) => {
            const response = await apiClient.post(`/friends/request/${targetUserId}`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to send friend request');
            }
            return await response.json();
        },
        
        acceptRequest: async (requestId) => {
            const response = await apiClient.post(`/friends/accept/${requestId}`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to accept friend request');
            }
            return await response.json();
        },
        
        declineRequest: async (requestId) => {
            const response = await apiClient.post(`/friends/decline/${requestId}`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to decline friend request');
            }
            return await response.json();
        },
        
        removeFriend: async (friendId) => {
            const response = await apiClient.delete(`/friends/${friendId}`);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to remove friend');
            }
            return await response.json();
        },
        
        getRecommendations: async (limit = 10) => {
            const response = await apiClient.get(`/friends/recommendations?limit=${limit}`);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to get recommendations');
            }
            return await response.json();
        }
    },
    
    groups: {
        createGroup: async (groupData) => {
            const response = await apiClient.post('/groups', groupData);
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to create group');
            }
            return await response.json();
        },
        
        getMyGroups: async () => {
            const response = await apiClient.get('/groups/my');
            if (!response.ok) throw new Error('Failed to fetch my groups');
            return await response.json();
        },
        
        getPublicGroups: async () => {
            const response = await apiClient.get('/groups/public');
            if (!response.ok) throw new Error('Failed to fetch public groups');
            return await response.json();
        },
        
        searchGroups: async (query) => {
            const response = await apiClient.get(`/groups/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('Failed to search groups');
            return await response.json();
        },
        
        joinGroup: async (groupId) => {
            const response = await apiClient.post(`/groups/${groupId}/join`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to join group');
            }
            return await response.json();
        },
        
        leaveGroup: async (groupId) => {
            const response = await apiClient.post(`/groups/${groupId}/leave`, {});
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to leave group');
            }
            return await response.json();
        },
        
        sendMessage: async (groupId, content) => {
            const response = await apiClient.post(`/groups/${groupId}/messages`, { content });
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to send message');
            }
            return await response.json();
        },
        
        getMessages: async (groupId, page = 0, size = 50) => {
            const response = await apiClient.get(`/groups/${groupId}/messages?page=${page}&size=${size}`);
            if (!response.ok) throw new Error('Failed to fetch messages');
            return await response.json();
        },
        
        getGroupDetails: async (groupId) => {
            const response = await apiClient.get(`/groups/${groupId}`);
            if (!response.ok) throw new Error('Failed to fetch group details');
            return await response.json();
        },
        
        getMembers: async (groupId) => {
            const response = await apiClient.get(`/groups/${groupId}/members`);
            if (!response.ok) throw new Error('Failed to fetch group members');
            return await response.json();
        }
    }
};

// Export for use in other files
window.API = API;
window.apiClient = apiClient;
window.checkAuth = checkAuth;
window.TokenManager = TokenManager;
window.API_BASE_URL = API_BASE_URL;
