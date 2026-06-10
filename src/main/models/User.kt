package org.nexus.openpress

enum class UserRoles(
    ADMIN,
    AUTHOR,
    CLERK,
    USER
)

data class User (
    val id: Int,
    val email: String,
    val password: String,
    val authStatus: Boolean,
    val role: UserRoles
)

