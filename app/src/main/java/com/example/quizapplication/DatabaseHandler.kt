package com.example.quizapplication

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHandler private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "QuizApplicationDatabase"
        const val DATABASE_VERSION = 3

        const val TABLE_USERS = "Users"

        const val KEY_USERID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_USER_PASSWORD = "password"
        const val KEY_USER_EMAIL = "email"
        const val KEY_USER_SCORE = "score"

        var databaseHandler: DatabaseHandler? = null     // Database handler object

        @Synchronized
        fun getInstance(context: Context): DatabaseHandler? {
            if (databaseHandler == null) databaseHandler =
                DatabaseHandler(context.applicationContext)
            return databaseHandler
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUserTable = "CREATE TABLE $TABLE_USERS(" +
                "$KEY_USERID INTEGER PRIMARY KEY, " +
                "$KEY_USERNAME TEXT NOT NULL," +
                "$KEY_USER_EMAIL TEXT NOT NULL," +
                "$KEY_USER_PASSWORD TEXT NOT NULL," +
                "$KEY_USER_SCORE INTEGER NOT NULL)"

        db?.execSQL(createUserTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
        }
    }

    fun signupUser(user: User): Int {
        val db = writableDatabase
        try {
            db.beginTransaction()

            var cursor = db.rawQuery(
                "SELECT * FROM $TABLE_USERS WHERE ($KEY_USERNAME = ?)",
                arrayOf(user.username)
            )
            if (cursor.moveToFirst()) {
                cursor.close()
                return -1
            }

            cursor = db.rawQuery(
                "SELECT * FROM $TABLE_USERS WHERE ($KEY_USER_EMAIL = ?)",
                arrayOf(user.email)
            )
            if (cursor.moveToFirst()) {
                cursor.close()
                return -2
            }
            cursor.close()

            val record = ContentValues()
            record.put(KEY_USERNAME, user.username)
            record.put(KEY_USER_EMAIL, user.email)
            record.put(KEY_USER_PASSWORD, user.password)
            record.put(KEY_USER_SCORE, user.score)
            db.insertOrThrow(TABLE_USERS, null, record)
            db.setTransactionSuccessful()
        } catch (ex: SQLiteException) {
            return -3
        } finally {
            db.endTransaction()
            db.close()
        }
        return 1
    }

    fun loginUser(user: User): Long {
        var userid: Long
        val db = readableDatabase
        try {
            db.beginTransaction()
            val cursor =
                if (user.email != "email") {

                    db.rawQuery(
                        "SELECT $KEY_USERID FROM $TABLE_USERS WHERE ($KEY_USER_EMAIL = ?) and ($KEY_USER_PASSWORD = ?)",
                        arrayOf(user.email, user.password)
                    )
                } else {

                    db.rawQuery(
                        "SELECT $KEY_USERID FROM $TABLE_USERS WHERE ($KEY_USERNAME = ?) and ($KEY_USER_PASSWORD = ?)",
                        arrayOf(user.username, user.password)
                    )
                }

            if (cursor.moveToFirst()) {
                userid = cursor.getLong(cursor.getColumnIndex(KEY_USERID))
                db.setTransactionSuccessful()
            } else userid = -1L
            cursor.close()
        } catch (ex: SQLiteException) {
            userid = -2L
        } finally {
            db.endTransaction()
            db.close()
        }
        return userid
    }

    fun getUser(userid: Long): User? {
        var user: User? = null
        val db = readableDatabase
        try {
            db.beginTransaction()
            val selectQuery = "SELECT * FROM $TABLE_USERS WHERE $KEY_USERID = $userid"

            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()) {
                user = User(
                    username = cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),
                    password = cursor.getString(cursor.getColumnIndex(KEY_USER_PASSWORD)),
                    email = cursor.getString(cursor.getColumnIndex(KEY_USER_EMAIL))
                )
            }
            cursor.close()
            db.setTransactionSuccessful()
        } catch (ex: SQLiteException) {
            Log.d("SQL_Exception", "SQLite exception thrown in DatabaseHandler getUserDetails")
        } finally {
            db.endTransaction()
            db.close()
        }
        return user
    }

    fun setUserScore(userid: Long, score: Int) {
        val db = writableDatabase
        var newScore = 0
        try {
            db.beginTransaction()
            var cursor =
                db.rawQuery(
                    "SELECT $KEY_USER_SCORE FROM $TABLE_USERS WHERE $KEY_USERID = $userid",
                    null
                )
            if (cursor.moveToFirst()) {
                newScore = cursor.getInt(cursor.getColumnIndex(KEY_USER_SCORE)) + score
            } else {
                Log.d("myError", "Error has occurred")
                return
            }
            cursor = db.rawQuery(
                "UPDATE $TABLE_USERS SET $KEY_USER_SCORE = ? WHERE $KEY_USERID = $userid",
                arrayOf(newScore.toString())
            )
            cursor.close()
            db.setTransactionSuccessful()
        } catch (ex: SQLiteException) {
            ex.printStackTrace()
        } finally {
            db.endTransaction()
            db.close()
        }
    }
}