package com.taskmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.taskmanagement.dto.Response;
import com.taskmanagement.model.ProjectStatus;
import com.taskmanagement.service.ProjectLifecycleService;

@ExtendWith(MockitoExtension.class)
class ProjectLifecycleControllerTest {

    private static final Long PROJECT_ID = 42L;

    @Mock private ProjectLifecycleService lifecycleService;

    @InjectMocks
    private ProjectLifecycleController controller;

    @Test
    void activateDelegatesToLifecycleService() {
        assertDelegates(ProjectStatus.ACTIVE, Command.ACTIVATE);
    }

    @Test
    void holdDelegatesToLifecycleService() {
        assertDelegates(ProjectStatus.ON_HOLD, Command.HOLD);
    }

    @Test
    void resumeDelegatesToLifecycleService() {
        assertDelegates(ProjectStatus.ACTIVE, Command.RESUME);
    }

    @Test
    void completeDelegatesToLifecycleService() {
        assertDelegates(ProjectStatus.COMPLETED, Command.COMPLETE);
    }

    @Test
    void archiveDelegatesToLifecycleService() {
        assertDelegates(ProjectStatus.ARCHIVED, Command.ARCHIVE);
    }

    private void assertDelegates(ProjectStatus status, Command command) {
        Response<ProjectStatus> serviceResponse = Response.success(status, "ok");
        when(command.invokeService(lifecycleService)).thenReturn(serviceResponse);

        ResponseEntity<Response<ProjectStatus>> response = command.invokeController(controller);

        assertThat(response.getBody()).isSameAs(serviceResponse);
        command.verifyInvocation(lifecycleService);
    }

    private enum Command {
        ACTIVATE {
            @Override Response<ProjectStatus> invokeService(ProjectLifecycleService service) {
                return service.activate(PROJECT_ID);
            }
            @Override ResponseEntity<Response<ProjectStatus>> invokeController(
                    ProjectLifecycleController controller
            ) {
                return controller.activate(PROJECT_ID);
            }
            @Override void verifyInvocation(ProjectLifecycleService service) {
                verify(service).activate(PROJECT_ID);
            }
        },
        HOLD {
            @Override Response<ProjectStatus> invokeService(ProjectLifecycleService service) {
                return service.hold(PROJECT_ID);
            }
            @Override ResponseEntity<Response<ProjectStatus>> invokeController(
                    ProjectLifecycleController controller
            ) {
                return controller.hold(PROJECT_ID);
            }
            @Override void verifyInvocation(ProjectLifecycleService service) {
                verify(service).hold(PROJECT_ID);
            }
        },
        RESUME {
            @Override Response<ProjectStatus> invokeService(ProjectLifecycleService service) {
                return service.resume(PROJECT_ID);
            }
            @Override ResponseEntity<Response<ProjectStatus>> invokeController(
                    ProjectLifecycleController controller
            ) {
                return controller.resume(PROJECT_ID);
            }
            @Override void verifyInvocation(ProjectLifecycleService service) {
                verify(service).resume(PROJECT_ID);
            }
        },
        COMPLETE {
            @Override Response<ProjectStatus> invokeService(ProjectLifecycleService service) {
                return service.complete(PROJECT_ID);
            }
            @Override ResponseEntity<Response<ProjectStatus>> invokeController(
                    ProjectLifecycleController controller
            ) {
                return controller.complete(PROJECT_ID);
            }
            @Override void verifyInvocation(ProjectLifecycleService service) {
                verify(service).complete(PROJECT_ID);
            }
        },
        ARCHIVE {
            @Override Response<ProjectStatus> invokeService(ProjectLifecycleService service) {
                return service.archive(PROJECT_ID);
            }
            @Override ResponseEntity<Response<ProjectStatus>> invokeController(
                    ProjectLifecycleController controller
            ) {
                return controller.archive(PROJECT_ID);
            }
            @Override void verifyInvocation(ProjectLifecycleService service) {
                verify(service).archive(PROJECT_ID);
            }
        };

        abstract Response<ProjectStatus> invokeService(ProjectLifecycleService service);
        abstract ResponseEntity<Response<ProjectStatus>> invokeController(
                ProjectLifecycleController controller
        );
        abstract void verifyInvocation(ProjectLifecycleService service);
    }
}
