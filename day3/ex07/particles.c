// emcc -s ASM_JS=1 particles.c -s INVOKE_RUN=0 -o resources/public/js/native.html

// emcc -O2 -o index.html -s ASM_JS=1 -s NO_FILESYSTEM=1 --closure 1 particles.c
// emcc -O2 -o index.html -s NO_FILESYSTEM=1 -s INVOKE_RUN=1 -s "EXPORT_NAME='Foo'" -s MODULARIZE=1 --closure 1 particles.c

// f=new Float32Array(Module.HEAPF32.buffer, Module.HEAP32[5247920>>2], 384)
// Module.ccall("dump",null,['ptr'],[5247920])

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <emscripten.h>

typedef struct {
  float x,y,z;
} Vec3;

typedef struct {
  Vec3 pos;
  Vec3 vel;
  uint32_t col;
} Particle;

typedef struct {
  Particle *particles;
  uint32_t numParticles;
  uint32_t maxParticles;
  Vec3 emitPos;
  Vec3 emitDir;
  Vec3 gravity;
} ParticleSystem;


Vec3* setVec3(Vec3 *v, float x, float y, float z) {
  v->x = x;
  v->y = y;
  v->z = z;
  return v;
}

Vec3* addVec3(Vec3* v, Vec3* v2) {
  v->x += v2->x;
  v->y += v2->y;
  v->z += v2->z;
  return v;
}

ParticleSystem* makeParticleSystem(uint32_t num) {
  ParticleSystem *psys = (ParticleSystem*)malloc(sizeof(ParticleSystem));
  psys->particles = (Particle*)malloc(num * sizeof(Particle));
  psys->maxParticles = num;
  psys->numParticles = 0;
  return psys;
}

void emitParticle(ParticleSystem* psys) {
  Particle *p = &psys->particles[psys->numParticles];
  p->pos = psys->emitPos;
  p->vel = psys->emitDir;
  p->vel.x += (float)rand() / (float)RAND_MAX * 2.0f - 1.0f;
  p->vel.z += (float)rand() / (float)RAND_MAX * 2.0f - 1.0f;
  p->col = (uint32_t)rand();
  psys->numParticles++;
}

EMSCRIPTEN_KEEPALIVE ParticleSystem* updateParticleSystem(ParticleSystem* psys) {
  if (psys->numParticles < psys->maxParticles) {
    emitParticle(psys);
  } else {
    psys->numParticles = 0;
  }
  for(uint32_t i=0; i < psys->numParticles; i++) {
    Particle *p = &psys->particles[i];
    addVec3(&(p->pos), &(p->vel));
    addVec3(&(p->pos), &(psys->gravity));
  }
  return psys;
}

EMSCRIPTEN_KEEPALIVE uint32_t getNumParticles(ParticleSystem* psys) {
  return psys->numParticles;
}

EMSCRIPTEN_KEEPALIVE float getParticleComponent(ParticleSystem* psys, uint32_t idx, uint32_t component) {
  Vec3 *pos = &((psys->particles[idx]).pos);
  switch(component) {
  case 0: return pos->x;
  case 1: return pos->y;
  case 2: return pos->z;
  default: return 0;
  }
}

EMSCRIPTEN_KEEPALIVE int main(int argc, char** argv) {
  printf("Hello Emscripten!");
  ParticleSystem *psys = makeParticleSystem(100);
  setVec3(&(psys->emitPos), 500.f, 1.f, 0.f);
  setVec3(&(psys->emitDir), 0.f, 1.f, 0.f);
  setVec3(&(psys->gravity), 0.f, -0.01f, 0.f);
  return (int)psys;
}
