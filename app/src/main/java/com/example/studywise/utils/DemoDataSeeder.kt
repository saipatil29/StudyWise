package com.example.studywise.utils

import com.example.studywise.data.database.StudyWiseDatabase
import com.example.studywise.data.entity.Badge
import com.example.studywise.data.entity.Quiz
import com.example.studywise.data.entity.QuizAttempt
import com.example.studywise.data.entity.QuizQuestion
import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import com.example.studywise.data.entity.UserPreferences
import com.example.studywise.domain.GamificationEngine
import java.util.Calendar

object DemoDataSeeder {

    suspend fun seedDemoData(database: StudyWiseDatabase) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Clear existing data
        database.subjectDao().clearAll()
        database.topicDao().clearAll()
        database.studySessionDao().clearAll()
        database.quizDao().clearQuizzes()
        database.quizDao().clearAttempts()
        database.badgeDao().clearAll()

        // 2. Insert Subjects
        // Mobile App Dev: Exam in 3 days, Prep: 40%, Hard
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 3)
        val madId = database.subjectDao().insertSubject(
            Subject(
                name = "Mobile Application Development",
                examDate = cal.timeInMillis,
                preparationPercentage = 40,
                difficulty = "HARD",
                dailyGoalMinutes = 50,
                description = "Android Architecture, MVVM, Room, Kotlin Coroutines, Navigation",
                colorHex = "#4F46E5"
            )
        )

        // DBMS: Exam in 7 days, Prep: 70%, Medium
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val dbmsId = database.subjectDao().insertSubject(
            Subject(
                name = "Database Management Systems",
                examDate = cal.timeInMillis,
                preparationPercentage = 70,
                difficulty = "MEDIUM",
                dailyGoalMinutes = 45,
                description = "Relational Algebra, SQL Queries, Normalization, ACID Transactions, B-Trees",
                colorHex = "#0D9488"
            )
        )

        // AI: Exam in 10 days, Prep: 35%, Medium
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 10)
        val aiId = database.subjectDao().insertSubject(
            Subject(
                name = "Artificial Intelligence",
                examDate = cal.timeInMillis,
                preparationPercentage = 35,
                difficulty = "MEDIUM",
                dailyGoalMinutes = 40,
                description = "Search Algorithms (A*, Minimax), Machine Learning Basics, Knowledge Graphs",
                colorHex = "#F59E0B"
            )
        )

        // Computer Networks: Exam in 14 days, Prep: 55%, Hard
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 14)
        val cnId = database.subjectDao().insertSubject(
            Subject(
                name = "Computer Networks",
                examDate = cal.timeInMillis,
                preparationPercentage = 55,
                difficulty = "HARD",
                dailyGoalMinutes = 60,
                description = "OSI 7-Layer Model, TCP/IP, Congestion Control, Subnetting, Routing Protocols",
                colorHex = "#8B5CF6"
            )
        )

        // 3. Insert Topics
        // Computer Networks topics
        val cnTopics = listOf(
            Topic(subjectId = cnId, name = "OSI 7-Layer Reference Model", completionPercentage = 100, masteryLevel = "MASTERED", lastReviewed = now - (86400000 * 2)),
            Topic(subjectId = cnId, name = "TCP 3-Way Handshake & Teardown", completionPercentage = 80, masteryLevel = "FAMILIAR", lastReviewed = now - (86400000 * 4), nextReviewDate = now - 1000), // Due today!
            Topic(subjectId = cnId, name = "Routing Protocols (OSPF, BGP, RIP)", completionPercentage = 45, masteryLevel = "FAMILIAR", lastReviewed = now - (86400000 * 5)),
            Topic(subjectId = cnId, name = "Congestion Control & TCP Reno", completionPercentage = 30, masteryLevel = "UNSTUDIED"),
            Topic(subjectId = cnId, name = "Network Layer & IPv4/IPv6 Subnetting", completionPercentage = 20, masteryLevel = "UNSTUDIED")
        )
        database.topicDao().insertTopics(cnTopics)

        // MAD topics
        val madTopics = listOf(
            Topic(subjectId = madId, name = "Android Activity & Fragment Lifecycle", completionPercentage = 80, masteryLevel = "FAMILIAR", lastReviewed = now - (86400000 * 3), nextReviewDate = now - 5000), // Due today!
            Topic(subjectId = madId, name = "MVVM Architecture with ViewBinding", completionPercentage = 60, masteryLevel = "FAMILIAR"),
            Topic(subjectId = madId, name = "Room Database, DAOs & Entities", completionPercentage = 35, masteryLevel = "UNSTUDIED"),
            Topic(subjectId = madId, name = "Kotlin Coroutines & Flow Pipelines", completionPercentage = 25, masteryLevel = "UNSTUDIED")
        )
        database.topicDao().insertTopics(madTopics)

        // DBMS topics
        val dbmsTopics = listOf(
            Topic(subjectId = dbmsId, name = "Entity Relationship Modeling & ERD", completionPercentage = 100, masteryLevel = "MASTERED", lastReviewed = now - 86400000),
            Topic(subjectId = dbmsId, name = "Relational Normalization (1NF, 2NF, 3NF, BCNF)", completionPercentage = 75, masteryLevel = "FAMILIAR"),
            Topic(subjectId = dbmsId, name = "ACID Properties & Concurrency Control", completionPercentage = 70, masteryLevel = "FAMILIAR"),
            Topic(subjectId = dbmsId, name = "Indexing & B+ Trees Storage", completionPercentage = 40, masteryLevel = "UNSTUDIED")
        )
        database.topicDao().insertTopics(dbmsTopics)

        // AI topics
        val aiTopics = listOf(
            Topic(subjectId = aiId, name = "Uninformed Search (BFS, DFS)", completionPercentage = 80, masteryLevel = "FAMILIAR"),
            Topic(subjectId = aiId, name = "Heuristic Search (A* Algorithm)", completionPercentage = 40, masteryLevel = "UNSTUDIED"),
            Topic(subjectId = aiId, name = "Adversarial Search (Minimax with Alpha-Beta)", completionPercentage = 20, masteryLevel = "UNSTUDIED")
        )
        database.topicDao().insertTopics(aiTopics)

        // 4. Insert Quizzes and Questions
        val cnQuizId = database.quizDao().insertQuiz(
            Quiz(subjectId = cnId, title = "Computer Networks Mastery Check", description = "Test your core networking knowledge across OSI and TCP/IP")
        )
        database.quizDao().insertQuestions(
            listOf(
                QuizQuestion(
                    quizId = cnQuizId,
                    questionText = "Which OSI layer is responsible for packet routing and forwarding across logical networks?",
                    optionA = "Physical Layer",
                    optionB = "Data Link Layer",
                    optionC = "Network Layer",
                    optionD = "Transport Layer",
                    correctOptionIndex = 2,
                    explanation = "The Network Layer (Layer 3) handles end-to-end packet routing, logical addressing (IP), and forwarding."
                ),
                QuizQuestion(
                    quizId = cnQuizId,
                    questionText = "Which flags are exchanged during the standard TCP 3-way handshake?",
                    optionA = "SYN, SYN-ACK, ACK",
                    optionB = "ACK, SYN, FIN",
                    optionC = "SYN, FIN, ACK",
                    optionD = "URG, ACK, RST",
                    correctOptionIndex = 0,
                    explanation = "TCP initiates connection with SYN from client, SYN-ACK from server, and ACK from client."
                ),
                QuizQuestion(
                    quizId = cnQuizId,
                    questionText = "What is the default port number for secure HTTP (HTTPS)?",
                    optionA = "80",
                    optionB = "443",
                    optionC = "8080",
                    optionD = "22",
                    correctOptionIndex = 1,
                    explanation = "Port 443 is universally assigned for TLS/SSL encrypted HTTPS traffic."
                )
            )
        )

        val dbmsQuizId = database.quizDao().insertQuiz(
            Quiz(subjectId = dbmsId, title = "Database Systems & SQL Quiz", description = "Evaluate your normalization and transaction fundamentals")
        )
        database.quizDao().insertQuestions(
            listOf(
                QuizQuestion(
                    quizId = dbmsQuizId,
                    questionText = "Which normal form requires eliminating transitive functional dependencies?",
                    optionA = "First Normal Form (1NF)",
                    optionB = "Second Normal Form (2NF)",
                    optionC = "Third Normal Form (3NF)",
                    optionD = "Boyce-Codd Normal Form (BCNF)",
                    correctOptionIndex = 2,
                    explanation = "3NF requires the relation to be in 2NF and have NO transitive dependencies on primary keys."
                ),
                QuizQuestion(
                    quizId = dbmsQuizId,
                    questionText = "What does the 'I' stand for in ACID transaction properties?",
                    optionA = "Integrity",
                    optionB = "Isolation",
                    optionC = "Iteration",
                    optionD = "Index",
                    correctOptionIndex = 1,
                    explanation = "Isolation guarantees that concurrent execution of transactions leaves the database in the same state as serial execution."
                )
            )
        )

        // 5. Insert Recent Study Sessions (Over past 5 days to demonstrate streak & charts)
        val dayMillis = 86400000L
        database.studySessionDao().insertSession(StudySession(subjectId = madId, durationMinutes = 35, date = now - (dayMillis * 4), completed = true))
        database.studySessionDao().insertSession(StudySession(subjectId = dbmsId, durationMinutes = 45, date = now - (dayMillis * 3), completed = true))
        database.studySessionDao().insertSession(StudySession(subjectId = cnId, durationMinutes = 50, date = now - (dayMillis * 2), completed = true))
        database.studySessionDao().insertSession(StudySession(subjectId = madId, durationMinutes = 30, date = now - dayMillis, completed = true))
        database.studySessionDao().insertSession(StudySession(subjectId = cnId, durationMinutes = 25, date = now, completed = true))

        // Record a demo quiz attempt
        database.quizDao().insertAttempt(
            QuizAttempt(
                quizId = cnQuizId,
                subjectId = cnId,
                score = 3,
                totalQuestions = 3,
                percentage = 100,
                attemptedAt = now - (dayMillis * 2)
            )
        )

        // 6. User Preferences & Badges
        database.userPreferencesDao().insertOrUpdatePreferences(
            UserPreferences(
                id = 1,
                studentName = "Alex Rivera",
                studentEmail = "alex.rivera@cs.university.edu",
                dailyGoalMinutes = 120,
                defaultFocusDurationMinutes = 25,
                notificationsEnabled = true,
                themeMode = "SYSTEM",
                totalXp = 480,
                streakDays = 5,
                lastStudyDateMillis = now
            )
        )

        val defaultBadges = GamificationEngine.getDefaultBadges().map { badge ->
            if (badge.id == "first_session") {
                badge.copy(isUnlocked = true, unlockedAt = now - (dayMillis * 4))
            } else {
                badge
            }
        }
        database.badgeDao().insertBadges(defaultBadges)
    }
}
