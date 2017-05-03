package org.reekwest.http.formats

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.lens.Invalid
import org.reekwest.http.lens.Meta
import org.reekwest.http.lens.Missing

open class JsonErrorResponseRendererContract<ROOT : NODE, NODE: Any>(val j: Json<ROOT, NODE>){

    @Test
    fun `can build 400`() {
        val response = JsonErrorResponseRenderer(j).badRequest(listOf(
            Missing(Meta(true, "location1", "name1")),
            Invalid(Meta(false, "location2", "name2"))))
        assertThat(response.bodyString(),
            equalTo("""{"message":"Missing/invalid parameters","params":[{"name":"name1","type":"location1","required":true,"reason":"Missing"},{"name":"name2","type":"location2","required":false,"reason":"Invalid"}]}"""))
    }

    @Test
    fun `can build 404`() {
        val response = JsonErrorResponseRenderer(j).notFound()
        assertThat(response.bodyString(),
            equalTo("""{"message":"No route found on this path. Have you used the correct HTTP verb?"}"""))
    }
}