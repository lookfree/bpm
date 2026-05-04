package org.jeecg.modules.bpm.definition;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.domain.entity.NodeConfig;
import org.jeecg.modules.bpm.mapper.FormBindingMapper;
import org.jeecg.modules.bpm.mapper.NodeConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefinitionCategoryServiceTest {

    BpmProcessDefinitionMapper defMapper;
    NodeConfigMapper nodeConfigMapper;
    FormBindingMapper formBindingMapper;
    DefinitionCategoryService svc;

    @BeforeEach
    void setUp() {
        defMapper = mock(BpmProcessDefinitionMapper.class);
        nodeConfigMapper = mock(NodeConfigMapper.class);
        formBindingMapper = mock(FormBindingMapper.class);
        svc = new DefinitionCategoryService(defMapper, nodeConfigMapper, formBindingMapper);
    }

    @Test
    void clone_setsSandboxCategory() {
        BpmProcessDefinition src = def("id1", "PUBLISHED");
        when(defMapper.selectById("id1")).thenReturn(src);
        when(nodeConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(formBindingMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(defMapper.insert(any())).thenAnswer(inv -> {
            BpmProcessDefinition d = inv.getArgument(0);
            d.setId("sandbox-uuid");
            return 1;
        });

        String sandboxId = svc.cloneAsSandbox("id1", "alice");

        assertThat(sandboxId).isEqualTo("sandbox-uuid");
        verify(defMapper).insert(argThat(d ->
            "SANDBOX".equals(d.getCategory()) && "DRAFT".equals(d.getState())
        ));
    }

    @Test
    void clone_setsDraftStateAndSandboxCategory() {
        BpmProcessDefinition src = def("id1", "PUBLISHED");
        when(defMapper.selectById("id1")).thenReturn(src);
        when(nodeConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(formBindingMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(defMapper.insert(any())).thenAnswer(inv -> {
            BpmProcessDefinition d = inv.getArgument(0);
            d.setId("sandbox-uuid-2");
            return 1;
        });

        svc.cloneAsSandbox("id1", "alice");

        verify(defMapper).insert(argThat(d ->
            "DRAFT".equals(d.getState()) &&
            "SANDBOX".equals(d.getCategory()) &&
            d.getDefKey() != null && d.getDefKey().contains("_sandbox_")
        ));
    }

    @Test
    void clone_archived_throws() {
        BpmProcessDefinition src = def("id2", "ARCHIVED");
        when(defMapper.selectById("id2")).thenReturn(src);

        assertThatThrownBy(() -> svc.cloneAsSandbox("id2", "alice"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clone_notFound_throws() {
        when(defMapper.selectById("nonexistent")).thenReturn(null);

        assertThatThrownBy(() -> svc.cloneAsSandbox("nonexistent", "alice"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("definition not found");
    }

    @Test
    void clone_clonesNodeConfigs() {
        BpmProcessDefinition src = def("id1", "PUBLISHED");
        when(defMapper.selectById("id1")).thenReturn(src);
        when(defMapper.insert(any())).thenAnswer(inv -> {
            BpmProcessDefinition d = inv.getArgument(0);
            d.setId("sandbox-uuid");
            return 1;
        });

        NodeConfig nc = new NodeConfig();
        nc.setId("nc-1");
        nc.setDefId("id1");
        nc.setNodeId("node1");
        nc.setMultiMode("PARALLEL");
        nc.setFormPerm("{\"field\":\"READ\"}");
        when(nodeConfigMapper.selectList(any())).thenReturn(List.of(nc));
        when(formBindingMapper.selectList(any())).thenReturn(Collections.emptyList());

        svc.cloneAsSandbox("id1", "alice");

        verify(nodeConfigMapper).insert(argThat(n ->
            "sandbox-uuid".equals(n.getDefId()) &&
            "node1".equals(n.getNodeId()) &&
            "PARALLEL".equals(n.getMultiMode())
        ));
    }

    @Test
    void clone_clonesFormBindings() {
        BpmProcessDefinition src = def("id1", "DRAFT");
        when(defMapper.selectById("id1")).thenReturn(src);
        when(defMapper.insert(any())).thenAnswer(inv -> {
            BpmProcessDefinition d = inv.getArgument(0);
            d.setId("sandbox-uuid");
            return 1;
        });
        when(nodeConfigMapper.selectList(any())).thenReturn(Collections.emptyList());

        FormBinding fb = new FormBinding();
        fb.setId("fb-1");
        fb.setDefId("id1");
        fb.setFormId("form-42");
        fb.setPurpose("APPLY");
        when(formBindingMapper.selectList(any())).thenReturn(List.of(fb));

        svc.cloneAsSandbox("id1", "alice");

        verify(formBindingMapper).insert(argThat(b ->
            "sandbox-uuid".equals(b.getDefId()) &&
            "form-42".equals(b.getFormId()) &&
            "APPLY".equals(b.getPurpose())
        ));
    }

    @Test
    void listByCategory_callsMapperWithFilter() {
        when(defMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<BpmProcessDefinition> result = svc.listByCategory("PROD");

        assertThat(result).isEmpty();
        verify(defMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void listAll_returnsAll() {
        BpmProcessDefinition d1 = def("id1", "PUBLISHED");
        BpmProcessDefinition d2 = def("id2", "DRAFT");
        when(defMapper.selectList(null)).thenReturn(List.of(d1, d2));

        List<BpmProcessDefinition> result = svc.listAll();

        assertThat(result).hasSize(2);
    }

    private BpmProcessDefinition def(String id, String state) {
        BpmProcessDefinition d = new BpmProcessDefinition();
        d.setId(id);
        d.setState(state);
        d.setDefKey("test-key");
        d.setName("Test Definition");
        d.setTenantId("default");
        d.setCategory("PROD");
        return d;
    }
}
