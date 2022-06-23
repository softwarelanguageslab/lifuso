import { defineNuxtConfig } from 'nuxt';

// https://v3.nuxtjs.org/api/configuration/nuxt.config
export default defineNuxtConfig({
  target: 'static',
  app: {
    head: {
      titleTemplate: 'LiFUSO | %s',
    },
  },
  css: ['@/assets/scss/main.scss'],
  modules: ['@nuxt/content'],
  content: {
    highlight: {
      theme: 'github-light',
      preload: ['java'],
    },
  },
});
