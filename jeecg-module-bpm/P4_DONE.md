# P4 Done

All P4 tasks completed:
- Task 1: DefinitionLifecycleService — 4-state machine (DRAFT/TESTING/PUBLISHED/ARCHIVED) with transition matrix (12 tests)
- Task 2: Two-step publish DRAFT→TESTING→PUBLISHED + history snapshot in BpmProcessDefinitionServiceImpl
- Task 3: DefinitionArchiveService + DefinitionRollbackService + DefinitionLifecycleController (/archive /rollback) + BpmExceptionHandler (400/409)
- Task 4: DistributedLock interface + NoOpDistributedLock (ReentrantLock, bpm.lock.enabled=false) + RedissonDistributedLock + RedissonConfig + ConcurrentPublishException (409)
- Task 5: DefinitionCategoryService (clone-as-sandbox, PROD/SANDBOX filter) + bpm_sandbox_run schema + /clone-as-sandbox endpoint
- Task 6: SandboxRun entity + SandboxRunMapper + SandboxRunService (start/appendLog/finish/findById)
- Task 7: SandboxService (shared engine, state=SANDBOX) + SandboxController (POST /sandbox/{defId}/start, GET /sandbox/{runId})
- Task 8: BpmNotificationDispatcher (silences SANDBOX instances) + SandboxNotificationSilenceTest
- Task 9: Full regression — 155 tests all green; fixed MapperScan for sandbox package, RedissonAutoConfig exclusion, two-step publish IT fixtures
- Task 10: Frontend snapshot — DefinitionStateBadge.vue, VersionHistoryPanel.vue, ConfirmDialog.vue, SandboxPage.vue, SandboxRunLog.vue, lifecycle.ts, sandbox.ts
- Task 11: P4_DONE.md + final regression + git push
