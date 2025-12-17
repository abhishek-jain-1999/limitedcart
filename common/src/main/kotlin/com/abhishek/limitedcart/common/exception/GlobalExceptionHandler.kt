package com.abhishek.limitedcart.common.exception

import com.abhishek.limitedcart.common.error.ErrorResponse
import com.abhishek.limitedcart.common.util.SharedUtils
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val details = ex.bindingResult.allErrors
            .mapNotNull {
                val fieldError = it as? FieldError
                fieldError?.field?.let { field -> "$field ${it.defaultMessage}" }
                    ?: it.defaultMessage
            }
            .joinToString(", ")

        val servletRequest = (request as? ServletWebRequest)?.request
        val body = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "ValidationError",
            message = details,
            correlationId = SharedUtils.getOrCreateCorrelationId(),
            path = servletRequest?.requestURI
        )

        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val statusCode = ex.statusCode
        val status = (statusCode as? HttpStatus) ?: HttpStatus.valueOf(statusCode.value())
        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.reason,
            correlationId = SharedUtils.getOrCreateCorrelationId(),
            path = request.requestURI
        )
        return ResponseEntity(body, status)
    }

    @ExceptionHandler(OutOfStockException::class)
    fun handleOutOfStock(
        ex: OutOfStockException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                status = HttpStatus.CONFLICT.value(),
                error = "OutOfStock",
                message = ex.message,
                correlationId = SharedUtils.getOrCreateCorrelationId(),
                path = request.requestURI
            )
        )

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(),
                error = "ResourceNotFound",
                message = ex.message,
                correlationId = SharedUtils.getOrCreateCorrelationId(),
                path = request.requestURI
            )
        )

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val correlationId = SharedUtils.getOrCreateCorrelationId()
        log.error(
            "Unhandled exception for path={} correlationId={}",
            request.requestURI,
            correlationId,
            ex
        )
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message,
            correlationId = correlationId,
            path = request.requestURI
        )
        return ResponseEntity(body, status)
    }
}
