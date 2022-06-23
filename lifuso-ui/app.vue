<template>
  <div class="layout">
    <main class="container">
      <NuxtPage />
    </main>
  </div>
</template>

<style lang="scss" scoped>
.layout {
  display: grid;
  grid-template-rows: auto 1fr auto;
}

.container {
  width: min(calc(100% - 2rem), 75em);
  margin-inline: auto;
  padding-top: var(--size-10);
  padding-bottom: var(--size-10);
}
</style>
