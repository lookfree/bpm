import type { AppRouteModule } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

const bpm: AppRouteModule = {
  path: '/bpm',
  name: 'Bpm',
  component: LAYOUT,
  redirect: '/bpm/definition',
  meta: { title: '流程配置', icon: 'ion:git-network-outline', orderNo: 100 },
  children: [
    {
      path: 'definition',
      name: 'BpmDefinition',
      component: () => import('/@/views/bpm/definition/DefinitionList.vue'),
      meta: { title: '流程定义' },
    },
    {
      path: 'designer',
      name: 'BpmDesigner',
      component: () => import('/@/views/bpm/designer/DesignerPage.vue'),
      meta: { title: '流程设计器', hideMenu: true, currentActiveMenu: '/bpm/definition' },
    },
    {
      path: 'todo',
      name: 'BpmTodo',
      component: () => import('/@/views/bpm/task/TodoListPage.vue'),
      meta: { title: '我的待办' },
    },
    {
      path: 'done',
      name: 'BpmDone',
      component: () => import('/@/views/bpm/task/DoneListPage.vue'),
      meta: { title: '我的已办' },
    },
    {
      path: 'task/approve/:taskId',
      name: 'BpmTaskApprove',
      component: () => import('/@/views/bpm/task/TaskApprovePage.vue'),
      meta: { hideMenu: true },
    },
    {
      path: 'instance/start/:defId',
      name: 'BpmInstanceStart',
      component: () => import('/@/views/bpm/instance/InstanceStartPage.vue'),
      meta: { hideMenu: true },
    },
    {
      path: 'form-binding',
      name: 'BpmFormBinding',
      component: () => import('/@/views/bpm/form-binding/FormBindingPage.vue'),
      meta: { title: '表单绑定' },
    },
  ],
};
export default bpm;
