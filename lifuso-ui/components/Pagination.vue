<script lang="ts" setup>
import { Icon } from '@iconify/vue';
import { ParsedContent } from '@nuxt/content/dist/runtime/types';

const props = defineProps({
  skip: {
    type: Number,
    required: true,
  },
  limit: {
    type: Number,
    required: true,
  },
  numberFeatures: {
    type: Number,
    required: true,
  },
});
defineEmits(['click:prev', 'click:next']);

const { data } = await useAsyncData('features', () =>
  queryContent<ParsedContent>('/features').find()
);

const hasPrev = computed(() => props.skip !== 0);
const hasNext = computed(
  () => props.numberFeatures - (props.skip + props.limit) > 0
);
</script>

<template>
  <div class="pagination">
    <button :disabled="!hasPrev" @click="$emit('click:prev')">
      <Icon icon="heroicons-solid:chevron-left" />
      Previous
    </button>
    <button :disabled="!hasNext" @click="$emit('click:next')">
      Next
      <Icon icon="heroicons-solid:chevron-right" />
    </button>
  </div>
</template>

<style lang="scss" scoped>
.pagination {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--size-3);
  place-items: center;
}
</style>
