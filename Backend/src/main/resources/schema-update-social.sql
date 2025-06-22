-- Update schema for social features
USE studia_db;

-- Update users table with social features
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS profile_image VARCHAR(500),
ADD COLUMN IF NOT EXISTS status_message VARCHAR(255),
ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP NULL;

-- Update friends table
ALTER TABLE friends 
ADD COLUMN IF NOT EXISTS declined_at TIMESTAMP NULL,
MODIFY COLUMN status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'BLOCKED') DEFAULT 'PENDING';

-- Create courses table if not exists
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    professor VARCHAR(100),
    semester VARCHAR(50),
    year INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Update study_groups table
ALTER TABLE study_groups 
ADD COLUMN IF NOT EXISTS course_id BIGINT,
ADD COLUMN IF NOT EXISTS owner_id BIGINT,
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
ADD CONSTRAINT fk_course_id FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL;

-- Update owner_id to use creator_id
UPDATE study_groups SET owner_id = creator_id WHERE owner_id IS NULL;

-- Update group_members table
ALTER TABLE group_members 
ADD COLUMN IF NOT EXISTS status ENUM('PENDING', 'ACTIVE', 'LEFT', 'KICKED') DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS invited_by BIGINT,
MODIFY COLUMN role ENUM('ADMIN', 'MODERATOR', 'MEMBER') DEFAULT 'MEMBER';

-- Update group_messages table
ALTER TABLE group_messages 
ADD COLUMN IF NOT EXISTS content VARCHAR(1000),
ADD COLUMN IF NOT EXISTS type ENUM('TEXT', 'FILE', 'IMAGE', 'SYSTEM', 'ANNOUNCEMENT') DEFAULT 'TEXT',
ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS reply_to_id BIGINT,
ADD CONSTRAINT fk_reply_to FOREIGN KEY (reply_to_id) REFERENCES group_messages(id) ON DELETE SET NULL;

-- Copy message to content column if content is empty
UPDATE group_messages SET content = message WHERE content IS NULL;

-- Create summaries table if not exists
CREATE TABLE IF NOT EXISTS summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (material_id) REFERENCES study_materials(id) ON DELETE CASCADE,
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create exams table if not exists
CREATE TABLE IF NOT EXISTS exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT,
    name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    time TIME,
    location VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL,
    INDEX idx_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_friends_user_friend ON friends(user_id, friend_id);
CREATE INDEX IF NOT EXISTS idx_group_members_status ON group_members(status);
CREATE INDEX IF NOT EXISTS idx_group_messages_type ON group_messages(type);
CREATE INDEX IF NOT EXISTS idx_users_online ON users(is_online);
CREATE INDEX IF NOT EXISTS idx_study_groups_active ON study_groups(is_active);

-- Create stored procedure for friend suggestions
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS get_friend_suggestions(IN user_id BIGINT, IN limit_count INT)
BEGIN
    -- Suggest friends of friends who are not already friends
    SELECT DISTINCT u.id, u.name, u.email, u.university, u.major, u.profile_image,
           COUNT(DISTINCT mutual.id) as mutual_friends_count
    FROM users u
    INNER JOIN friends f2 ON (
        (f2.user_id = u.id OR f2.friend_id = u.id) 
        AND f2.status = 'ACCEPTED'
    )
    INNER JOIN friends f1 ON (
        (f1.user_id = user_id OR f1.friend_id = user_id) 
        AND f1.status = 'ACCEPTED'
        AND ((f1.user_id = f2.user_id AND f1.friend_id != u.id) 
             OR (f1.friend_id = f2.user_id AND f1.user_id != u.id)
             OR (f1.user_id = f2.friend_id AND f1.friend_id != u.id) 
             OR (f1.friend_id = f2.friend_id AND f1.user_id != u.id))
    )
    LEFT JOIN friends mutual ON (
        ((mutual.user_id = user_id AND mutual.friend_id = u.id) 
         OR (mutual.friend_id = user_id AND mutual.user_id = u.id))
        AND mutual.status IN ('ACCEPTED', 'PENDING')
    )
    WHERE u.id != user_id 
      AND u.is_active = TRUE
      AND mutual.id IS NULL
    GROUP BY u.id
    ORDER BY mutual_friends_count DESC, u.name
    LIMIT limit_count;
END//
DELIMITER ;
