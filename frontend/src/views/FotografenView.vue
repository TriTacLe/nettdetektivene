<script setup lang="ts">
import SceneComponent from '../components/SceneComponent.vue'
import FotografenComponent from '../components/FotografenComponent.vue'
import type { FotografenChallenge } from '../types'

// Placeholder — replace with backend fetch when API is ready
const challenge: FotografenChallenge = {
  alternatives: [
    {
      imagePath: '/FotografenTest.jpg',
      description: 'En person med uvanlig mange fingre og merkelig bakgrunn',
      correctType: 'ai-generert',
      begrunnelser: [
        'Fingrene ser unaturlige ut',
        'Bakgrunnen virker generert',
        'Lyset stemmer ikke',
        'Detaljene mangler konsistens',
      ],
    },
    {
      imagePath: '/FotografenTest.jpg',
      description: 'Skolebarn som leker i skolegård, lett bevegelsesuskarphet',
      correctType: 'ekte',
      begrunnelser: [
        'Naturlig bevegelsesuskarphet',
        'Realistiske skygger',
        'Konsistente detaljer',
        'Ingen tegn på redigering',
      ],
    },
    {
      imagePath: '/FotografenTest.jpg',
      description: 'Kjent politiker på ukjent sted – bakgrunnen virker lagt inn',
      correctType: 'manipulert',
      begrunnelser: [
        'Bakgrunnen ser innklipt ut',
        'Lyset stemmer ikke mellom person og bakgrunn',
        'Kantene virker redigerte',
        'Fargene er unaturlige',
      ],
    },
    {
      imagePath: '/FotografenTest.jpg',
      description: 'Dyr i et naturbilde – lyset ser unaturlig ut',
      correctType: 'ai-generert',
      begrunnelser: [
        'Lyset er urealistisk',
        'Pelsen mangler naturlig tekstur',
        'Bakgrunnen er for perfekt',
        'Detaljene ser genererte ut',
      ],
    },
  ],
}
</script>

<template>
  <SceneComponent title="Fotografen" previous="/game-map" background="/FotografenBackground.png">
    <FotografenComponent :challenge="challenge" />
  </SceneComponent>
</template>
