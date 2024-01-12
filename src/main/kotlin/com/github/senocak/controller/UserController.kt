package com.github.senocak.controller

import com.github.senocak.domain.Document
import com.github.senocak.domain.DocumentUploadEvent
import com.github.senocak.domain.User
import com.github.senocak.domain.dto.ExceptionDto
import com.github.senocak.domain.dto.FileResponse
import com.github.senocak.domain.dto.PaginationCriteria
import com.github.senocak.domain.dto.UpdateUserDto
import com.github.senocak.domain.dto.UserPaginationDTO
import com.github.senocak.domain.dto.UserResponse
import com.github.senocak.exception.ServerException
import com.github.senocak.security.Authorize
import com.github.senocak.service.DocumentService
import com.github.senocak.service.UserService
import com.github.senocak.util.AppConstants.ADMIN
import com.github.senocak.util.AppConstants.USER
import com.github.senocak.util.AppConstants.securitySchemeName
import com.github.senocak.util.OmaErrorMessageType
import com.github.senocak.util.PageRequestBuilder
import com.github.senocak.util.convertEntityToDto
import com.github.senocak.util.logger
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Pattern
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.multipart.MultipartFile

@Validated
@RestController
@Authorize(roles = [ADMIN, USER])
@RequestMapping(UserController.URL)
@Tag(name = "User", description = "User Controller")
class UserController(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val documentService: DocumentService,
    private val eventPublisher: ApplicationEventPublisher,
    private val restTemplate: RestTemplate
): BaseController() {
    private val log: Logger by logger()

    @Throws(ServerException::class)
    @Operation(
        summary = "All Users",
        tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = UserPaginationDTO::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = securitySchemeName, scopes = [ADMIN])]
    )
    @Authorize(roles = [ADMIN])
    @GetMapping
    fun allUsers(
        @Parameter(name = "page", description = "Page number", example = "0") @RequestParam(defaultValue = "1", required = false) page: Int,
        @Parameter(name = "size", description = "Page size", example = "20") @RequestParam(defaultValue = "\${spring.data.web.pageable.default-page-size:10}", required = false) size: Int,
        @Parameter(name = "sortBy", description = "Sort by column", example = "id") @RequestParam(defaultValue = "id", required = false) sortBy: String,
        @Parameter(name = "sort", description = "Sort direction", schema = Schema(type = "string", allowableValues = ["asc", "desc"])) @RequestParam(defaultValue = "asc", required = false) @Pattern(regexp = "asc|desc") sort: String,
        @Parameter(name = "q", description = "Search keyword", example = "lorem") @RequestParam(required = false) q: String?
    ): UserPaginationDTO =
        arrayListOf("id", "name", "email")
            .run {
                if (this.none { it == sortBy }) {
                    "Invalid sort column"
                        .also { log.error(it) }
                        .run error@ { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                            variables = arrayOf(this@error), statusCode = HttpStatus.BAD_REQUEST) }
                }
                PaginationCriteria(page = page, size = size)
                    .also { it: PaginationCriteria ->
                        it.sortBy = sortBy
                        it.sort = sort
                        it.columns = this
                    }
                    .run paginationCriteria@ {
                        userService.findAllUsers(
                            specification = userService.createSpecificationForUser(q = q),
                            pageRequest = PageRequestBuilder.build(paginationCriteria = this@paginationCriteria)
                        )
                    }
                    .run messagePage@ {
                        UserPaginationDTO(
                            pageModel = this@messagePage,
                            items = this@messagePage.content.map { it: User -> it.convertEntityToDto() }.toList(),
                            sortBy = sortBy,
                            sort = sort
                        )
                    }
            }

    @Throws(ServerException::class)
    @Operation(
        summary = "Get me",
        tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = UserResponse::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = securitySchemeName, scopes = [ADMIN, USER])]
    )
    @GetMapping("/me")
    fun me(): UserResponse = userService.loggedInUser.run { this.convertEntityToDto(roles = true) }

    @PatchMapping("/me")
    @Operation(
        summary = "Update user",
        tags = ["User"],
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = UserResponse::class)))),
            ApiResponse(responseCode = "500", description = "internal server error occurred",
                content = arrayOf(Content(mediaType = "application/json", schema = Schema(implementation = ExceptionDto::class))))
        ],
        security = [SecurityRequirement(name = securitySchemeName, scopes = [ADMIN, USER])]
    )
    @Throws(ServerException::class)
    fun patchMe(request: HttpServletRequest,
        @Parameter(description = "Request body to update", required = true) @Validated @RequestBody userDto: UpdateUserDto,
        resultOfValidation: BindingResult
    ): UserResponse {
        validate(resultOfValidation = resultOfValidation)
        val user: User = userService.loggedInUser
        val name: String? = userDto.name
        if (!name.isNullOrEmpty())
            user.name = name
        val password: String? = userDto.password
        val passwordConfirmation: String? = userDto.passwordConfirmation
        if (!password.isNullOrEmpty()) {
            if (passwordConfirmation.isNullOrEmpty()) {
                "Password confirmation not provided"
                    .apply { log.error(this) }
                    .apply { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                        variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
            }
            if (passwordConfirmation != password) {
                "Password and confirmation not matched"
                    .apply { log.error(this) }
                    .apply { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                        variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
            }
            user.password = passwordEncoder.encode(password)
        }
        return userService.save(user = user).convertEntityToDto(roles = true)
    }

    @PostMapping("/document")
    fun handleFileUpload(
        @RequestParam("file") file: MultipartFile,
    ): FileResponse {
        val loggedInUser: User = userService.loggedInUser
        val uploadFileForBucket: ResponseEntity<FileResponse> = restTemplate.postForEntity<FileResponse>("http://localhost:8085/buckets/${loggedInUser.id}/files", file)
        when {
            uploadFileForBucket.statusCode.is4xxClientError -> {
                "4xx Error occured"
                    .apply { log.error(this) }
                    .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.BASIC_INVALID_INPUT,
                        variables = arrayOf(this), statusCode = HttpStatus.valueOf(uploadFileForBucket.statusCode.value())) }
            }
            uploadFileForBucket.statusCode.is5xxServerError -> {
                "5xx Error occured"
                    .apply { log.error(this) }
                    .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.GENERIC_SERVICE_ERROR,
                        variables = arrayOf(this), statusCode = HttpStatus.valueOf(uploadFileForBucket.statusCode.value())) }
            }
            !uploadFileForBucket.statusCode.is2xxSuccessful -> {
                "Undefined Error occured"
                    .apply { log.error(this) }
                    .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.GENERIC_SERVICE_ERROR,
                        variables = arrayOf(this), statusCode = HttpStatus.BAD_REQUEST) }
            }
        }
        val uploadFile: FileResponse = uploadFileForBucket.body
            ?: "Response is empty"
                .apply { log.error(this) }
                .run { throw ServerException(omaErrorMessageType = OmaErrorMessageType.GENERIC_SERVICE_ERROR,
                    variables = arrayOf(this), statusCode = HttpStatus.valueOf(uploadFileForBucket.statusCode.value())) }
        val document: Document = Document()
            .also { it: Document ->
                it.fileName = uploadFile.fileName
                it.fileSize = uploadFile.fileSize
                it.contentType = uploadFile.contentType
                it.createdAt = uploadFile.createdTime
                it.user = loggedInUser
            }
            .run {
                documentService.save(document = this)
            }
        //eventPublisher.publishEvent(DocumentUploadEvent(source = this, document = document))
        return uploadFile
    }

    companion object {
        const val URL = "/api/v1/user"
    }
}