package com.github.senocak.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.senocak.domain.dto.KafkaAction
import com.github.senocak.domain.dto.KafkaMessageTemplate
import com.github.senocak.kafka.producer.BucketCreateRequestKafkaProducer
import com.github.senocak.repository.DocumentPagesRepository
import com.github.senocak.repository.DocumentRepository
import com.github.senocak.repository.RoleRepository
import com.github.senocak.repository.UserRepository
import com.github.senocak.util.RoleName
import com.github.senocak.util.logger
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile


@Component
class Listener(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val documentRepository: DocumentRepository,
    private val documentPagesRepository: DocumentPagesRepository,
    private val bucketCreateRequestKafkaProducer: BucketCreateRequestKafkaProducer
){
    private val log: Logger by logger()
    private val jacksonObjectMapper: ObjectMapper = jacksonObjectMapper()

    @Value("\${spring.jpa.hibernate.ddl-auto}")
    lateinit var ddl: String

    @EventListener(ApplicationReadyEvent::class)
    fun applicationReadyEvent(event: ApplicationReadyEvent) {
        log.info("Time: ${event.timeTaken.seconds}")
        if (ddl == "create" || ddl == "create-drop") {
            roleRepository.deleteAll()
            userRepository.deleteAll()

            val userRole: Role = roleRepository.save(Role().also { it.name = RoleName.ROLE_USER })
            val adminRole: Role = roleRepository.save(Role().also { it.name = RoleName.ROLE_ADMIN })
            val defaultPass: String = passwordEncoder.encode("asenocak")
            val user1: User = userRepository.save(User().also { it.name = "Anıl Şenocak"; it.email = "anil@senocak.com"; it.password = defaultPass; it.roles = arrayListOf(userRole, adminRole)})
            val user2: User = userRepository.save(User().also { it.name = "Canela Skin"; it.email = "canela@skin.com"; it.password = defaultPass; it.roles = arrayListOf(userRole)})

            if (bucketCreateRequestKafkaProducer.running) {
                KafkaMessageTemplate(action = KafkaAction.MAKE_BUCKET, value = "${user1.id}")
                    .run { bucketCreateRequestKafkaProducer.produce(msg = jacksonObjectMapper.writeValueAsString(this)) }
                //KafkaMessageTemplate(action = KafkaAction.MAKE_BUCKET, value = "${user2.id}")
                //    .run { bucketCreateRequestKafkaProducer.produce(msg = jacksonObjectMapper.writeValueAsString(this)) }
            }

            val doc1: Document = documentRepository.save(Document()
                .also { it: Document ->
                    it.fileName = "filename1"
                    it.author = "author1"
                    it.keywords = "keyword1, keyword2"
                    it.subject = "subject1"
                    it.createdOn = LocalDateTime.now()
                    it.updatedOn = LocalDateTime.now()
                    it.description = "description1"
                    it.fileSize = 111
                    it.contentType = "pdf"
                    it.numberOfPages = 100
                    it.isEncrypted = false
                    it.user = user1
                }
            )
            val doc2: Document = documentRepository.save(Document()
                .also { it: Document ->
                    it.fileName = "filename2"
                    it.author = "author2"
                    it.keywords = "keyword3, keyword4"
                    it.subject = "subject2"
                    it.createdOn = LocalDateTime.now()
                    it.updatedOn = LocalDateTime.now()
                    it.description = "description2"
                    it.fileSize = 222
                    it.contentType = "pdf"
                    it.numberOfPages = 200
                    it.isEncrypted = true
                    it.user = user2
                }
            )
            //minioService.makeBucket(bucket = "${user1.id}")
            //    .takeUnless {
            //        minioService.bucketExists(bucketName = "${user1.id}")
            //    }
            //    .run { ClassPathResource("static/Ahmet Haşim Köse _ Ahmet Öncü - Tahsildarlar ve Borçlular.pdf").getFile() }
            //    .run { minioService.uploadFile(multipartFile = InMemoryMultipartFile(name = this.name, content = this.readBytes()), bucketName = "${user1.id}") }
//
            //val file2: File = ClassPathResource("static/Alan Musgrave - Sağduyu, Bilim ve Şüphecilik.pdf").getFile()
            //minioService.makeBucket(bucket = "${user2.id}")
            //    .takeUnless { minioService.bucketExists(bucketName = "${user2.id}") }
            //    .run { minioService.uploadFile(multipartFile = InMemoryMultipartFile(name = file2.name, content = file2.readBytes()), bucketName = "${user2.id}") }
            //    .run {
            //        val document: PDDocument = Loader.loadPDF(file2.readBytes())
            //        val dpi = 300f
            //        val imageType: ImageType = ImageType.RGB
            //        extractPagesFromPdfAsImage(document = document, dpi = dpi, imageType = imageType)
            //            .forEachIndexed { index: Int, img: ByteArray ->
            //                    documentPagesRepository.save(DocumentPage()
            //                        .also { it: DocumentPage ->
            //                            it.page = index + 1
            //                            it.image = String(bytes = img, charset = Charset.forName("UTF-8"))
            //                            it.dpi = "$dpi"
            //                            it.imageType = imageType
            //                            it.document = doc2
            //                        })
            //            }
            //        document.close()
            //    }
            //
            log.info("[ApplicationReadyEvent]: db migrated.")
        }
    }
    /*
        @KafkaListener(
            topics = ["\${spring.kafka.producer.topic.bucket-create}"],
            groupId = "\${spring.application.name}"
        )
        fun consume(
            @Header("from") from: String,
            @Payload event: String,
            @Header(KafkaHeaders.RECEIVED_KEY) key: String,
            @Header(KafkaHeaders.RECEIVED_PARTITION) partition: String,
            @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
            @Header(KafkaHeaders.TIMESTAMP_TYPE) tst: String,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) ts: String,
            @Header(KafkaHeaders.CORRELATION_ID) cid: String?,
            @Header(KafkaHeaders.REPLY_TOPIC) rt: String?,
        ) {
            log.info("from: $from, event: $event, key: $key, partition: $partition, topic: $topic, tst: $tst, ts: $ts, cid: $cid, rt: $rt")
        }

        @KafkaListener(
            topics = ["\${spring.kafka.consumer.topic.bucket-create}"],
            groupId = "\${spring.application.name}"
        )
        fun listener(record: ConsumerRecord<String, String>) {
            log.info("${record.key()},${record.value()},${record.partition()},${record.topic()},${record.offset()}")
        }


        @Async
        @EventListener(value = [DocumentUploadEvent::class])
        fun userDocumentUploadEvent(event: DocumentUploadEvent) {
            MDC.put("documentId", "${event.document.id}")
            log.info("[DocumentUploadEvent] id:${event.document.id}")
            //if (event.document.contentType == "pdf")

            val inputStream: InputStream = minioService.downloadObject(event.document.user.id.toString(), event.document.id.toString())

            val document: PDDocument = Loader.loadPDF(inputStream.readAllBytes())
            val pages: Int = document.numberOfPages
            val documentInformation: PDDocumentInformation = document.documentInformation

            val pdfRenderer = PDFRenderer(document)
            val bim: BufferedImage = pdfRenderer.renderImageWithDPI(1, 300f, ImageType.RGB)
            //for (page in 0 until document.numberOfPages) {
            //    val bim = pdfRenderer.renderImageWithDPI(page, 300f, ImageType.RGB)
            //    val baos = ByteArrayOutputStream()
            //    ImageIO.write(bim, "png", baos)
            //    val imageBytes: ByteArray = baos.toByteArray()
            //    //ImageIOUtil.writeImage(bim, pdfFilename + "-" + (page + 1) + ".png", 300)
            //}
            log.info("bim: $bim")
            document.close()
            val mapOf = mapOf(
                "pages" to pages,
                "author" to documentInformation.author,
                "title" to documentInformation.title,
                "subject" to documentInformation.subject,
                "creationDate" to documentInformation.creationDate,
                "keywords" to documentInformation.keywords,
                "modificationDate" to documentInformation.modificationDate,
                "producer" to documentInformation.producer,
                "trapped" to documentInformation.trapped,
            )
        }
    */
    private fun extractPagesFromPdfAsImage(document: PDDocument, dpi: Float = 300f, imageType: ImageType = ImageType.RGB): ArrayList<ByteArray> {
        val pdfRenderer = PDFRenderer(document)
        val byteArrayList: ArrayList<ByteArray> = arrayListOf()
        for (page: Int in 0 until document.numberOfPages) {
            val bim: BufferedImage = pdfRenderer.renderImageWithDPI(page, dpi, imageType)
            val baos = ByteArrayOutputStream()
            ImageIO.write(bim, "png", baos)
            byteArrayList.add(element = baos.toByteArray())
        }
        return byteArrayList
    }

}

class InMemoryMultipartFile(private val name: String, private val content: ByteArray) : MultipartFile {
    override fun getName(): String = name
    override fun getOriginalFilename(): String = name
    override fun getContentType(): String = "application/octet-stream"
    override fun isEmpty(): Boolean = content.isEmpty()
    override fun getSize(): Long = content.size.toLong()
    @Throws(IOException::class)
    override fun getBytes(): ByteArray = content
    @Throws(IOException::class)
    override fun getInputStream(): InputStream = ByteArrayInputStream(content)
    @Throws(IOException::class)
    override fun transferTo(dest: File): Unit  = TODO()
}