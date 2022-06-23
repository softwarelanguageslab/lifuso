<script lang="ts" setup>
defineProps({
  modelValue: {
    type: String,
    default: '',
  },
});
defineEmits(['update:modelValue']);
</script>

<template>
  <label>
    <span class="sr-only">Search</span>
    <input
      :value="modelValue"
      type="search"
      name="q"
      placeholder="Search Feature"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  </label>
</template>

<style lang="scss" scoped>
label,
input[type='search'] {
  width: 100%;
}

@media screen and (min-width: 1024px) {
  label {
    width: 70%;
  }
}

@media screen and (min-width: 1440px) {
  label {
    width: 50%;
  }
}
</style>
