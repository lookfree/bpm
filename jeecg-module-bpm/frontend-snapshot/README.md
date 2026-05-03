# Frontend Snapshot

This directory is an **archived snapshot** of frontend Vue files that belong in the parallel
`jeecgboot-vue3` repository. They are stored here for reference and code review alongside
the BPM backend module.

## Target path in jeecgboot-vue3

```
src/views/bpm/designer/
  BpmnDesigner.vue   — bpmn-js Modeler wrapper component (v-model, ready/error events)
  DesignerPage.vue   — page with save/publish actions, calls /@/api/bpm/definition
```

## npm dependencies to install in jeecgboot-vue3

```bash
npm install bpmn-js@^17.0.0
# optional typings:
npm install --save-dev @types/bpmn-js@^17.0.0
```

## Parallel-commit instruction

These files must be **manually committed** in the `jeecgboot-vue3` repo:

```bash
cd $VUE_REPO
cp -r <bpm-repo>/jeecg-module-bpm/frontend-snapshot/src/views/bpm/designer src/views/bpm/
git add src/views/bpm/designer
git commit -m "feat(bpm-p1): bpmn-js 17 designer wrapper + designer page"
```

`/@/api/bpm/definition` API module is created in Task 11 (Definition list page).

---

## Task 11 additions — Definition list + API layer + routes

All files mirror the corresponding paths under `$VUE_REPO/src/`:

```
src/api/bpm/model/definitionModel.ts   — TypeScript types (DefinitionVO, PageResult<T>, VersionVO, …)
src/api/bpm/definition.ts              — HTTP helpers (listDefinitions, getDefinition, createDefinition,
                                          updateDefinition, deleteDefinition, publishDefinition, listVersions)
src/views/bpm/definition/
  DefinitionList.data.ts               — STATE_OPTIONS + VxeTable columns config
  DefinitionList.vue                   — List page with filter bar, vxe-table, pagination, VersionsModal
  components/VersionsModal.vue         — a-modal showing listVersions rows
src/router/routes/modules/bpm.ts       — AppRouteModule: /bpm → BpmDefinition + BpmDesigner children
```

### Routes registered

| name | path | notes |
|------|------|-------|
| `BpmDefinition` | `/bpm/definition` | main list page |
| `BpmDesigner` | `/bpm/designer` | `hideMenu: true`, `currentActiveMenu: '/bpm/definition'` |
