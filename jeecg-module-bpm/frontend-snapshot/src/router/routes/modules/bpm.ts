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
  ],
};
export default bpm;
