package com.github.senocak.repository

import com.github.senocak.domain.Role
import com.github.senocak.domain.User
import com.github.senocak.util.RoleName
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface RoleRepository: PagingAndSortingRepository<Role, UUID>, CrudRepository<Role, UUID> {
    fun findByName(roleName: RoleName): Role?
}

interface UserRepository:
    PagingAndSortingRepository<User, UUID>,
    CrudRepository<User, UUID>,
    JpaRepository<User, UUID>,
    JpaSpecificationExecutor<User> {
    fun findByEmail(email: String?): User?
    fun existsByEmail(email: String?): Boolean
}
