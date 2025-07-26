package com.ohana.shared.enums

enum class TagPermissionType {
    /**
     * User can view all tags except those in the exception list
     * Example: Adult can view all tags except "work" and "bob's work"
     */
    ALLOW_ALL_EXCEPT,

    /**
     * User can only view tags in the exception list
     * Example: Child can only view "kids" tag
     */
    DENY_ALL_EXCEPT,
}
