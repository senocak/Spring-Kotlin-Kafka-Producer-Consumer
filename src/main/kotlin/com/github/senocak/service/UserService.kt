package com.github.senocak.service

import com.github.senocak.domain.User
import com.github.senocak.repository.UserRepository
import com.github.senocak.util.RoleName
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
): UserDetailsService {

    fun findAllUsers(specification: Specification<User>, pageRequest: Pageable): Page<User> =
        userRepository.findAll(specification, pageRequest)

    /**
     * @param id -- uuid id to find in db
     * @return -- Optional User object
     */
    fun findById(id: UUID): User =
        userRepository.findById(id).orElseThrow { UsernameNotFoundException("User not found with id") }

    /**
     * @param email -- string email to find in db
     * @return -- true or false
     */
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmail(email = email)

    /**
     * @param email -- string email to find in db
     * @return -- User object
     * @throws UsernameNotFoundException -- throws UsernameNotFoundException
     */
    @Throws(UsernameNotFoundException::class)
    fun findByEmail(email: String): User =
        userRepository.findByEmail(email = email) ?: throw UsernameNotFoundException("User not found with email: $email")

    /**
     * @param user -- User object to persist to db
     * @return -- User object that is persisted to db
     */
    fun save(user: User): User = userRepository.save(user)

    /**
     * @param email -- email
     * @return -- Spring User object
     */
    @Transactional
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): org.springframework.security.core.userdetails.User {
        val user: User = findByEmail(email = email)
        val authorities: List<GrantedAuthority> = user.roles.stream()
            .map { SimpleGrantedAuthority(RoleName.fromString(r = it.name.toString())!!.name) }
            .toList()
        return org.springframework.security.core.userdetails.User(user.email, user.password, authorities)
    }

    /**
     * @return -- User entity that is retrieved from context
     */
    val loggedInUser: User
        get() =
            (SecurityContextHolder.getContext().authentication.principal as org.springframework.security.core.userdetails.User).username
                .run { findByEmail(email = this)  }

    fun createSpecificationForUser(q: String?): Specification<User> {
        return Specification { root: Root<User>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
            val predicates: MutableList<Predicate> = ArrayList()
            if (!q.isNullOrEmpty()) {
                val predicateName: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%${q.lowercase()}%")
                val predicateEmail: Predicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%${q.lowercase()}%")
                predicates.add(criteriaBuilder.or(predicateName, predicateEmail))
            }
            query.where(*predicates.toTypedArray()).distinct(true).restriction
        }
    }
}
