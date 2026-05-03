package org.jeecg.modules.bpm.service.form;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.mapper.FormBindingMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FormBindingServiceTest {

    FormBindingMapper mapper = mock(FormBindingMapper.class);
    FormBindingService service = new FormBindingService(mapper);

    @Test void bindInsertsAndReturnsId() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any())).thenReturn(1);
        String id = service.bind("def1", "form1", "APPLY");
        assertThat(id).isNotNull();
        verify(mapper).insert(any(FormBinding.class));
    }

    @Test void bindIdempotentReturnsExistingId() {
        FormBinding existing = new FormBinding();
        existing.setId("existing-id");
        when(mapper.selectOne(any())).thenReturn(existing);
        String id = service.bind("def1", "form1", "APPLY");
        assertThat(id).isEqualTo("existing-id");
        verify(mapper, never()).insert(any());
    }

    @Test void listByDefReturnsAll() {
        FormBinding fb = new FormBinding();
        when(mapper.selectList(any())).thenReturn(List.of(fb));
        assertThat(service.listByDef("def1")).hasSize(1);
    }

    @Test void unbindDeletesById() {
        service.unbind("binding-id");
        verify(mapper).deleteById("binding-id");
    }
}
