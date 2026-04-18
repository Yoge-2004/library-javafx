import re

with open('src/main/java/com/example/entities/UsersDB.java', 'r') as f:
    content = f.read()

# Add check to addUser(User user)
pattern = r'(public void addUser\(User user\) throws UserException \{\s+if \(user == null\) \{\s+throw new UserException\("User cannot be null"\);\s+\}\s+String userId = user\.getUserId\(\);\s+if \(userId == null \|\| userId\.trim\(\)\.isEmpty\(\)\) \{\s+throw new UserException\("User ID cannot be empty"\);\s+\}\s+lock\.writeLock\(\)\.lock\(\);\s+try \{)'

replacement = r'\1\n            if (users.containsKey(userId.trim())) {\n                throw new UserException("User already exists: " + userId.trim());\n            }'

new_content = re.sub(pattern, replacement, content)

with open('src/main/java/com/example/entities/UsersDB.java', 'w') as f:
    f.write(new_content)
