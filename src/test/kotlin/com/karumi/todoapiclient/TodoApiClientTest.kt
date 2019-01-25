package com.karumi.todoapiclient

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import todoapiclient.Either
import todoapiclient.TodoApiClient
import todoapiclient.dto.TaskDto
import todoapiclient.exception.ItemNotFoundError
import todoapiclient.exception.NetworkError
import todoapiclient.exception.UnknownApiError

class TodoApiClientTest : MockWebServerTest() {

    private lateinit var apiClient: TodoApiClient

    @Before
    override fun setUp() {
        super.setUp()
        val mockWebServerEndpoint = baseEndpoint
        apiClient = TodoApiClient(mockWebServerEndpoint)
    }

    @Test
    fun sendsAcceptAndContentTypeHeaders() {
        enqueueMockResponse(200, "getTasksResponse.json")

        apiClient.allTasks

        assertRequestContainsHeader("Accept", "application/json")
    }

    @Test
    fun sendsGetAllTaskRequestToTheCorrectEndpoint() {
        enqueueMockResponse(200, "getTasksResponse.json")

        apiClient.allTasks

        assertGetRequestSentTo("/todos")
    }

    @Test
    fun parsesTasksProperlyGettingAllTheTasks() {
        enqueueMockResponse(200, "getTasksResponse.json")

        val tasks = apiClient.allTasks.right!!

        assertEquals(200, tasks.size.toLong())
        assertTaskContainsExpectedValues(tasks[0])
    }

    @Test
    fun shouldReturnUnauthorizedIf403WhenGettingTasks() {
        enqueueMockResponse(403)

        val error = apiClient.allTasks.left

        assertEquals(UnknownApiError(403), error)
    }

    @Test
    fun shouldReturnErrorIf500WhenGettingTasks() {
        enqueueMockResponse(500)

        val error = apiClient.allTasks.left

        assertEquals(UnknownApiError(500), error)
    }

    @Test
    fun shouldReturnEmptyListWhenGettingTasks() {
        enqueueMockResponse(200, "getTasksEmptyResponse.json")

        val emptyList = apiClient.allTasks.right!!

        assertEquals(0, emptyList.size)
    }

    @Test
    fun shouldParseCorrectlyWhenGettingTaskById() {
        enqueueMockResponse(200, "getTaskByIdResponse.json")

        val task = apiClient.getTaskById("1").right!!

        assertEquals(
            givenAnyTask(),
            task
        )
    }

    @Test
    fun shouldSendGetTaskByIdToTheCorrectEndPoint() {
        enqueueMockResponse(200, "getTaskByIdResponse.json")

        apiClient.getTaskById("1")

        assertGetRequestSentTo("/todos/1")
    }

    @Test
    fun shouldReturnNotFoundWhenGettingNotExistingTask() {
        enqueueMockResponse(404)

        val task = apiClient.getTaskById("2").left

        assertEquals(ItemNotFoundError, task)
    }

    @Test
    fun shouldReturnErrorIf500WhenGettingTaskById() {
        enqueueMockResponse(500)

        val error = apiClient.getTaskById("1").left

        assertEquals(UnknownApiError(500), error)
    }

    @Test
    fun shouldReturnMalformedJsonWhenGettingTaskById() {
        enqueueMockResponse(200, "getTaskByIdMalformedResponse.json")

        val result = apiClient.getTaskById("1")

        assertEquals(Either.Left(NetworkError), result)
    }

    @Test
    fun shouldCreateTaskCorrectly() {
        enqueueMockResponse(201, "updateTaskResponse.json")

        val expected = givenAnyTask()
        val task = apiClient.updateTaskById(expected).right!!

        assertEquals(expected, task)
    }

    @Test
    fun shouldReturnBadRequest() {
        enqueueMockResponse(400)

        val error = apiClient.updateTaskById(givenAnyTask()).left!!

        assertEquals(UnknownApiError(400), error)
    }

    @Test
    fun shouldSendCorrectRequestToUpdateTask() {
        enqueueMockResponse(201, "updateTaskResponse.json")

        apiClient.updateTaskById(
            TaskDto(
                id = "1",
                userId = "2",
                title = "Finish this kata",
                isFinished = false
            )
        )

        assertRequestBodyEquals("updateTaskRequest.json")
    }

    @Test
    fun shouldReturnServerErrorWhenUpdatingTask() {
        enqueueMockResponse(500)

        val error = apiClient.updateTaskById(givenAnyTask()).left

        assertEquals(UnknownApiError(500), error)
    }

    private fun givenAnyTask() =
        TaskDto(
            id = "1",
            userId = "1",
            title = "delectus aut autem",
            isFinished = false
        )

    private fun assertTaskContainsExpectedValues(task: TaskDto?) {
        assertTrue(task != null)
        assertEquals(task?.id, "1")
        assertEquals(task?.userId, "1")
        assertEquals(task?.title, "delectus aut autem")
        assertFalse(task!!.isFinished)
    }
}
