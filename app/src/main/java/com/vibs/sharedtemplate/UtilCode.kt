package com.vibs.sharedtemplate

import android.content.Context
import com.google.gson.Gson
import okhttp3.*
import okio.Buffer
import java.io.IOException

class GraphQLInterceptor(private val context: Context) : Interceptor {
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val methodAnnotation = request.tag(Invocation::class.java)?.method()?.getAnnotation(GraphQLQuery::class.java)
        if (methodAnnotation == null) return chain.proceed(request) // Skip if no annotation

        // Read the GraphQL query from the assets folder
        val query = readGraphQLQueryFromAssets(context, methodAnnotation.fileName)

        // Parse the original request body to get variables
        val originalBody = parseRequestBody(request.body)
        val newBody = GraphQLRequest(query, originalBody?.variables)

        // Create a new request with the injected query
        val newRequestBody = gson.toJson(newBody)
            .toRequestBody("application/json".toMediaTypeOrNull())

        val newRequest = request.newBuilder()
            .post(newRequestBody)
            .build()

        return chain.proceed(newRequest)
    }

    private fun parseRequestBody(requestBody: RequestBody?): GraphQLRequest? {
        return requestBody?.let {
            val buffer = Buffer()
            it.writeTo(buffer)
            val json = buffer.readUtf8()
            gson.fromJson(json, GraphQLRequest::class.java)
        }
    }

    private fun readGraphQLQueryFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}