package com.github.senocak.domain

import com.github.senocak.util.RoleName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID

@MappedSuperclass
open class BaseDomain(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP") var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP") var updatedAt: LocalDateTime = LocalDateTime.now()
) : Serializable

@Entity
@Table(name = "users", uniqueConstraints = [
    UniqueConstraint(columnNames = ["email"])
])
@EntityListeners(value = [UserListener::class])
class User: BaseDomain() {
    @Column(nullable = false) lateinit var name: String
    @Column(nullable = false) lateinit var email: String
    @Column(nullable = false) var password: String? = null
    @JoinTable(name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    @ManyToMany(fetch = FetchType.EAGER)
    var roles: List<Role> = arrayListOf()

    override fun toString(): String = "User(name='$name', email='$email', password=$password, roles=$roles)"
}

@Entity
@Table(name = "roles")
class Role: BaseDomain() {
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    lateinit var name: RoleName
}
